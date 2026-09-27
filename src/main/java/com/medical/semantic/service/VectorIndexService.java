package com.medical.semantic.service;

import com.medical.semantic.domain.MedicalDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量索引服务
 *
 * 专门负责向量索引的创建、更新、删除
 * 通过监听文档事件实现与 DocumentService 的解耦
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    private final EmbeddingService embeddingService;
    private final com.medical.semantic.repository.MilvusRepository milvusRepository;

    /**
     * 监听文档创建事件，自动创建向量索引
     */
    @EventListener
    public void handleDocumentCreated(DocumentService.DocumentCreatedEvent event) {
        List<MedicalDocument> documents = event.getDocuments();
        log.info("监听到文档创建事件, 开始创建向量索引, 数量: {}", documents.size());

        indexDocuments(documents);
    }

    /**
     * 监听文档更新事件，自动更新向量索引
     */
    @EventListener
    public void handleDocumentUpdated(DocumentService.DocumentUpdatedEvent event) {
        MedicalDocument document = event.getDocument();
        log.info("监听到文档更新事件, 开始更新向量索引, ID: {}", document.getId());

        // 删除旧索引
        deleteIndex(document.getId());

        // 创建新索引
        indexDocuments(List.of(document));
    }

    /**
     * 监听文档删除事件，自动删除向量索引
     */
    @EventListener
    public void handleDocumentDeleted(DocumentService.DocumentDeletedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("监听到文档删除事件, 开始删除向量索引, ID: {}", documentId);

        deleteIndex(documentId);
    }

    /**
     * 批量索引文档
     *
     * @param documents 文档列表
     */
    public void indexDocuments(List<MedicalDocument> documents) {
        log.info("批量索引文档, 数量: {}", documents.size());

        try {
            // 1. 生成向量
            List<String> texts = documents.stream()
                    .map(doc -> doc.getTitle() + " " + doc.getContent())
                    .toList();

            List<List<Float>> vectors = embeddingService.embedBatch(texts);

            // 2. 插入 Milvus
            List<Long> docIds = documents.stream()
                    .map(MedicalDocument::getId)
                    .toList();

            milvusRepository.insertVectorBatch(docIds, vectors);

            log.info("文档索引完成");
        } catch (Exception e) {
            log.error("文档索引失败", e);
            // 注意: 这里不抛出异常，避免影响文档保存事务
            // 实际生产环境可以加入重试机制或告警
        }
    }

    /**
     * 删除文档索引
     *
     * @param documentId 文档 ID
     */
    public void deleteIndex(Long documentId) {
        log.info("删除文档索引: {}", documentId);

        try {
            milvusRepository.deleteVector(documentId);
        } catch (Exception e) {
            log.error("删除向量索引失败: {}", documentId, e);
        }
    }

    /**
     * 重建全部索引
     *
     * 注意: 此方法需要访问 DocumentService，但仅用于管理操作，不形成循环依赖
     */
    public void rebuildAllIndexes(List<MedicalDocument> allDocuments) {
        log.info("开始重建全部索引, 文档数量: {}", allDocuments.size());

        // 1. 清空 Milvus Collection
        milvusRepository.clearCollection();

        // 2. 批量索引
        indexDocuments(allDocuments);

        log.info("索引重建完成");
    }
}
