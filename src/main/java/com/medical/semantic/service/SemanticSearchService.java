package com.medical.semantic.service;

import com.medical.semantic.domain.MedicalDocument;
import com.medical.semantic.domain.SearchResult;
import com.medical.semantic.repository.DocumentMapper;
import com.medical.semantic.repository.MilvusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 语义搜索服务
 *
 * 使用向量数据库实现基于语义相似度的智能检索
 *
 * 修复说明: 移除对 DocumentService 的依赖，直接使用 DocumentMapper
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticSearchService {

    private final EmbeddingService embeddingService;
    private final MilvusRepository milvusRepository;
    private final DocumentMapper documentMapper;

    /**
     * 语义搜索
     *
     * @param query 查询文本
     * @param topK 返回最相似的 K 个结果
     * @param threshold 相似度阈值 (0-1)
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, int topK, float threshold) {
        log.info("执行语义搜索: query='{}', topK={}, threshold={}", query, topK, threshold);

        // 1. 将查询文本转换为向量
        List<Float> queryVector = embeddingService.embed(query);
        log.debug("查询向量维度: {}", queryVector.size());

        // 2. 在 Milvus 中搜索相似向量
        List<SearchResult> results = milvusRepository.search(queryVector, topK);

        // 3. 过滤低于阈值的结果
        List<SearchResult> filteredResults = results.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        log.info("搜索完成, 返回 {} 个结果", filteredResults.size());

        // 4. 补充文档详细信息
        // TODO: 从 MySQL 获取文档元数据

        return filteredResults;
    }

    /**
     * 相似文档推荐
     *
     * @param documentId 文档 ID
     * @param topK 返回最相似的 K 个文档
     * @return 相似文档列表
     */
    public List<SearchResult> findSimilar(Long documentId, int topK) {
        log.info("查找相似文档: documentId={}, topK={}", documentId, topK);

        // 1. 获取文档内容（直接使用 Mapper，避免依赖 Service）
        MedicalDocument document = documentMapper.findById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在: " + documentId);
        }

        // 2. 使用文档内容作为查询
        String query = document.getTitle() + " " + document.getContent();

        // 3. 执行语义搜索
        List<SearchResult> results = search(query, topK + 1, 0.0f);

        // 4. 排除自身
        return results.stream()
                .filter(r -> !documentId.equals(r.getDocument().getId()))
                .limit(topK)
                .toList();
    }

    /**
     * 相似病例推荐
     *
     * @param caseDescription 病例描述
     * @param topK 返回最相似的 K 个病例
     * @return 相似病例列表
     */
    public List<SearchResult> findSimilarCases(String caseDescription, int topK) {
        log.info("查找相似病例: caseDescription='{}', topK={}",
                 caseDescription.substring(0, Math.min(50, caseDescription.length())), topK);

        // 执行语义搜索，只返回病例类型的文档
        List<SearchResult> results = search(caseDescription, topK * 2, 0.6f);

        // 过滤出病例类型
        return results.stream()
                .filter(r -> MedicalDocument.DocumentType.CASE.equals(
                        r.getDocument().getType()))
                .limit(topK)
                .toList();
    }

}
