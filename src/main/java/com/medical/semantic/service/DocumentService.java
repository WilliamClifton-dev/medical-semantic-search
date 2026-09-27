package com.medical.semantic.service;

import com.medical.semantic.domain.MedicalDocument;
import com.medical.semantic.repository.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档管理服务
 *
 * 修复说明: 移除对 SemanticSearchService 的直接依赖，改用事件发布
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建文档
     */
    @Transactional
    public MedicalDocument create(MedicalDocument document) {
        log.info("创建文档: {}", document.getTitle());

        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentMapper.insert(document);

        // 发布文档创建事件，由 VectorIndexService 监听并创建索引
        eventPublisher.publishEvent(new DocumentCreatedEvent(this, List.of(document)));

        return document;
    }

    /**
     * 批量创建文档
     */
    @Transactional
    public void createBatch(List<MedicalDocument> documents) {
        log.info("批量创建文档, 数量: {}", documents.size());

        LocalDateTime now = LocalDateTime.now();
        documents.forEach(doc -> {
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            documentMapper.insert(doc);
        });

        // 发布批量创建事件
        eventPublisher.publishEvent(new DocumentCreatedEvent(this, documents));
    }

    /**
     * 根据 ID 查询文档
     */
    public MedicalDocument findById(Long id) {
        return documentMapper.findById(id);
    }

    /**
     * 查询所有文档
     */
    public List<MedicalDocument> findAll() {
        return documentMapper.findAll();
    }

    /**
     * 根据类型查询文档
     */
    public List<MedicalDocument> findByType(MedicalDocument.DocumentType type) {
        return documentMapper.findByType(type.name());
    }

    /**
     * 更新文档
     */
    @Transactional
    public MedicalDocument update(MedicalDocument document) {
        log.info("更新文档: {}", document.getId());

        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.update(document);

        // 发布文档更新事件
        eventPublisher.publishEvent(new DocumentUpdatedEvent(this, document));

        return document;
    }

    /**
     * 删除文档
     */
    @Transactional
    public void delete(Long id) {
        log.info("删除文档: {}", id);

        // 发布文档删除事件
        eventPublisher.publishEvent(new DocumentDeletedEvent(this, id));

        // 删除文档记录
        documentMapper.delete(id);
    }

    /**
     * 关键词搜索（MySQL 全文索引）
     */
    public List<MedicalDocument> keywordSearch(String query) {
        log.info("关键词搜索: {}", query);
        return documentMapper.keywordSearch(query);
    }

    /**
     * 统计信息
     */
    public long countByType(MedicalDocument.DocumentType type) {
        return documentMapper.countByType(type.name());
    }

    public long countTotal() {
        return documentMapper.countTotal();
    }

    // ==================== 事件定义 ====================

    /**
     * 文档创建事件
     */
    public static class DocumentCreatedEvent {
        private final Object source;
        private final List<MedicalDocument> documents;

        public DocumentCreatedEvent(Object source, List<MedicalDocument> documents) {
            this.source = source;
            this.documents = documents;
        }

        public List<MedicalDocument> getDocuments() {
            return documents;
        }
    }

    /**
     * 文档更新事件
     */
    public static class DocumentUpdatedEvent {
        private final Object source;
        private final MedicalDocument document;

        public DocumentUpdatedEvent(Object source, MedicalDocument document) {
            this.source = source;
            this.document = document;
        }

        public MedicalDocument getDocument() {
            return document;
        }
    }

    /**
     * 文档删除事件
     */
    public static class DocumentDeletedEvent {
        private final Object source;
        private final Long documentId;

        public DocumentDeletedEvent(Object source, Long documentId) {
            this.source = source;
            this.documentId = documentId;
        }

        public Long getDocumentId() {
            return documentId;
        }
    }
}
