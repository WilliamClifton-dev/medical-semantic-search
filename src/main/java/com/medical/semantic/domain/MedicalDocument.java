package com.medical.semantic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 医学文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalDocument {

    /**
     * 文档 ID
     */
    private Long id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 文档类型: LITERATURE(文献), CASE(病例), GUIDELINE(指南)
     */
    private DocumentType type;

    /**
     * 专业领域: CARDIOLOGY(心血管), ONCOLOGY(肿瘤), NEUROLOGY(神经)等
     */
    private String specialty;

    /**
     * 关键词（逗号分隔）
     */
    private String keywords;

    /**
     * 作者
     */
    private String author;

    /**
     * 发布日期
     */
    private LocalDateTime publishDate;

    /**
     * 来源/出处
     */
    private String source;

    /**
     * 向量 ID（Milvus 中的 ID）
     */
    private Long vectorId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        LITERATURE("文献"),
        CASE("病例"),
        GUIDELINE("指南");

        private final String description;

        DocumentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
