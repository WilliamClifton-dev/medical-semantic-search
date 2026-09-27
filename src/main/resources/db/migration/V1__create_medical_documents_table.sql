-- 创建医学文档表
CREATE TABLE medical_documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档 ID',
    title VARCHAR(500) NOT NULL COMMENT '文档标题',
    content TEXT NOT NULL COMMENT '文档内容',
    type VARCHAR(50) NOT NULL COMMENT '文档类型: LITERATURE, CASE, GUIDELINE',
    specialty VARCHAR(100) COMMENT '专业领域',
    keywords VARCHAR(500) COMMENT '关键词',
    author VARCHAR(200) COMMENT '作者',
    publish_date DATETIME COMMENT '发布日期',
    source VARCHAR(500) COMMENT '来源',
    vector_id BIGINT COMMENT 'Milvus 向量 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_type (type),
    INDEX idx_specialty (specialty),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_search (title, content, keywords) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='医学文档表';
