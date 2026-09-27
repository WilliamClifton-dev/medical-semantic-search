package com.medical.semantic.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    /**
     * 文档信息
     */
    private MedicalDocument document;

    /**
     * 相似度分数 (0-1, 越高越相似)
     */
    private Float score;

    /**
     * 匹配的片段（高亮显示）
     */
    private List<String> highlights;

    /**
     * 搜索方法: KEYWORD, SEMANTIC, HYBRID
     */
    private String searchMethod;
}
