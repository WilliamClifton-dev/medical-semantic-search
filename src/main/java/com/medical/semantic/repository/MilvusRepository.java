package com.medical.semantic.repository;

import com.medical.semantic.domain.SearchResult;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.SearchResults;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Milvus 向量数据库访问层
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MilvusRepository {

    private final MilvusServiceClient milvusClient;

    @Value("${milvus.collection.name}")
    private String collectionName;

    @Value("${milvus.collection.dimension}")
    private Integer dimension;

    @Value("${milvus.collection.index-type}")
    private String indexType;

    @Value("${milvus.collection.metric-type}")
    private String metricType;

    private static final String FIELD_ID = "id";
    private static final String FIELD_VECTOR = "embedding";
    private static final String FIELD_DOC_ID = "doc_id";

    /**
     * 初始化: 创建 Collection 和索引
     */
    @PostConstruct
    public void initialize() {
        try {
            // 检查 Collection 是否存在
            boolean hasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .build()
            ).getData();

            if (!hasCollection) {
                createCollection();
                createIndex();
                log.info("Milvus Collection 创建成功: {}", collectionName);
            } else {
                log.info("Milvus Collection 已存在: {}", collectionName);
            }

            // 加载 Collection 到内存
            loadCollection();

        } catch (Exception e) {
            log.error("初始化 Milvus Collection 失败", e);
            throw new RuntimeException("Milvus 初始化失败", e);
        }
    }

    /**
     * 创建 Collection
     */
    private void createCollection() {
        // 定义字段
        FieldType fieldId = FieldType.newBuilder()
                .withName(FIELD_ID)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType fieldVector = FieldType.newBuilder()
                .withName(FIELD_VECTOR)
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        FieldType fieldDocId = FieldType.newBuilder()
                .withName(FIELD_DOC_ID)
                .withDataType(DataType.Int64)
                .build();

        // 创建 Schema
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("医学文档向量库")
                .addFieldType(fieldId)
                .addFieldType(fieldVector)
                .addFieldType(fieldDocId)
                .build();

        milvusClient.createCollection(createParam);
        log.info("Collection 创建成功: {}", collectionName);
    }

    /**
     * 创建向量索引
     */
    private void createIndex() {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName(FIELD_VECTOR)
                .withIndexType(io.milvus.param.IndexType.valueOf(indexType))
                .withMetricType(io.milvus.param.MetricType.valueOf(metricType))
                .withExtraParam("{\"M\": 16, \"efConstruction\": 256}")
                .build();

        milvusClient.createIndex(indexParam);
        log.info("索引创建成功: {} - {}", indexType, metricType);
    }

    /**
     * 加载 Collection 到内存
     */
    private void loadCollection() {
        milvusClient.loadCollection(
            LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build()
        );
        log.info("Collection 已加载到内存");
    }

    /**
     * 插入向量
     *
     * @param docId 文档 ID
     * @param vector 向量
     * @return Milvus 内部 ID
     */
    public Long insertVector(Long docId, List<Float> vector) {
        List<Long> docIds = new ArrayList<>();
        docIds.add(docId);

        List<List<Float>> vectors = new ArrayList<>();
        vectors.add(vector);

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                    new InsertParam.Field(FIELD_DOC_ID, docIds),
                    new InsertParam.Field(FIELD_VECTOR, vectors)
                ))
                .build();

        milvusClient.insert(insertParam);
        log.debug("向量插入成功, docId: {}", docId);

        return docId;
    }

    /**
     * 批量插入向量
     *
     * @param docIds 文档 ID 列表
     * @param vectors 向量列表
     */
    public void insertVectorBatch(List<Long> docIds, List<List<Float>> vectors) {
        if (docIds.size() != vectors.size()) {
            throw new IllegalArgumentException("docIds 和 vectors 数量不匹配");
        }

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(Arrays.asList(
                    new InsertParam.Field(FIELD_DOC_ID, docIds),
                    new InsertParam.Field(FIELD_VECTOR, vectors)
                ))
                .build();

        milvusClient.insert(insertParam);
        log.info("批量插入向量成功, 数量: {}", docIds.size());
    }

    /**
     * 向量相似度搜索
     *
     * @param queryVector 查询向量
     * @param topK 返回最相似的 K 个结果
     * @return 文档 ID 和相似度分数列表
     */
    public List<SearchResult> search(List<Float> queryVector, int topK) {
        List<List<Float>> queryVectors = new ArrayList<>();
        queryVectors.add(queryVector);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(io.milvus.param.MetricType.valueOf(metricType))
                .withTopK(topK)
                .withVectors(queryVectors)
                .withVectorFieldName(FIELD_VECTOR)
                .withOutFields(List.of(FIELD_DOC_ID))
                .build();

        SearchResults response = milvusClient.search(searchParam).getData();
        SearchResultsWrapper wrapper = new SearchResultsWrapper(response);

        List<SearchResult> results = new ArrayList<>();

        for (int i = 0; i < wrapper.getRowRecords(0).size(); i++) {
            SearchResultsWrapper.IDScore idScore = wrapper.getIDScore(0, i);
            Long docId = (Long) wrapper.getFieldData(FIELD_DOC_ID, 0).get(i);

            SearchResult result = SearchResult.builder()
                    .score(idScore.getScore())
                    .searchMethod("SEMANTIC")
                    .build();

            results.add(result);
            log.debug("搜索结果: docId={}, score={}", docId, idScore.getScore());
        }

        return results;
    }

    /**
     * 删除向量
     *
     * @param docId 文档 ID
     */
    public void deleteVector(Long docId) {
        String expr = String.format("%s == %d", FIELD_DOC_ID, docId);

        milvusClient.delete(
            io.milvus.param.dml.DeleteParam.newBuilder()
                .withCollectionName(collectionName)
                .withExpr(expr)
                .build()
        );

        log.debug("向量删除成功, docId: {}", docId);
    }

    /**
     * 清空 Collection
     */
    public void clearCollection() {
        milvusClient.releaseCollection(
            ReleaseCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build()
        );

        milvusClient.dropCollection(
            DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build()
        );

        log.info("Collection 已清空并重建");
        initialize();
    }

    /**
     * 获取 Collection 统计信息
     */
    public void getCollectionStats() {
        var stats = milvusClient.getCollectionStatistics(
            GetCollectionStatisticsParam.newBuilder()
                .withCollectionName(collectionName)
                .build()
        );

        log.info("Collection 统计: {}", stats.getData());
    }
}
