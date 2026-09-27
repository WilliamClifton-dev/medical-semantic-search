package com.medical.semantic.controller;

import com.medical.semantic.domain.MedicalDocument;
import com.medical.semantic.domain.SearchResult;
import com.medical.semantic.service.DocumentService;
import com.medical.semantic.service.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "搜索", description = "医学文献搜索相关接口")
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final DocumentService documentService;

    /**
     * 语义搜索
     */
    @PostMapping("/semantic")
    @Operation(summary = "语义搜索", description = "基于向量数据库的语义相似度搜索")
    public ApiResponse<List<SearchResult>> semanticSearch(
            @RequestBody @Validated SemanticSearchRequest request) {

        log.info("收到语义搜索请求: {}", request.getQuery());

        List<SearchResult> results = semanticSearchService.search(
                request.getQuery(),
                request.getTopK(),
                request.getThreshold()
        );

        return ApiResponse.success(results);
    }

    /**
     * 关键词搜索
     */
    @PostMapping("/keyword")
    @Operation(summary = "关键词搜索", description = "基于 MySQL 全文索引的传统关键词搜索")
    public ApiResponse<List<MedicalDocument>> keywordSearch(
            @RequestBody @Validated KeywordSearchRequest request) {

        log.info("收到关键词搜索请求: {}", request.getQuery());

        List<MedicalDocument> results = documentService.keywordSearch(request.getQuery());

        return ApiResponse.success(results);
    }

    /**
     * 对比搜索（同时使用两种方法）
     */
    @PostMapping("/compare")
    @Operation(summary = "对比搜索", description = "同时执行关键词搜索和语义搜索，对比效果")
    public ApiResponse<ComparisonResult> compareSearch(
            @RequestBody @Validated CompareSearchRequest request) {

        log.info("收到对比搜索请求: {}", request.getQuery());

        // 关键词搜索
        long keywordStart = System.currentTimeMillis();
        List<MedicalDocument> keywordResults = documentService.keywordSearch(request.getQuery());
        long keywordTime = System.currentTimeMillis() - keywordStart;

        // 语义搜索
        long semanticStart = System.currentTimeMillis();
        List<SearchResult> semanticResults = semanticSearchService.search(
                request.getQuery(),
                request.getTopK(),
                0.0f
        );
        long semanticTime = System.currentTimeMillis() - semanticStart;

        ComparisonResult result = new ComparisonResult();
        result.setQuery(request.getQuery());
        result.setKeywordResults(keywordResults);
        result.setKeywordCount(keywordResults.size());
        result.setKeywordTimeMs(keywordTime);
        result.setSemanticResults(semanticResults);
        result.setSemanticCount(semanticResults.size());
        result.setSemanticTimeMs(semanticTime);

        return ApiResponse.success(result);
    }

    /**
     * 相似病例推荐
     */
    @PostMapping("/similar-cases")
    @Operation(summary = "相似病例推荐", description = "根据病例描述查找相似的历史病例")
    public ApiResponse<List<SearchResult>> findSimilarCases(
            @RequestBody @Validated SimilarCaseRequest request) {

        log.info("收到相似病例推荐请求");

        List<SearchResult> results = semanticSearchService.findSimilarCases(
                request.getCaseDescription(),
                request.getTopK()
        );

        return ApiResponse.success(results);
    }

    /**
     * 相似文档推荐
     */
    @GetMapping("/similar/{documentId}")
    @Operation(summary = "相似文档推荐", description = "基于指定文档查找相似文档")
    public ApiResponse<List<SearchResult>> findSimilarDocuments(
            @PathVariable Long documentId,
            @Parameter(description = "返回结果数量") @RequestParam(defaultValue = "5") int topK) {

        log.info("收到相似文档推荐请求: documentId={}", documentId);

        List<SearchResult> results = semanticSearchService.findSimilar(documentId, topK);

        return ApiResponse.success(results);
    }

    // ==================== 请求/响应模型 ====================

    @Data
    public static class SemanticSearchRequest {
        @NotBlank(message = "查询内容不能为空")
        private String query;

        @Min(value = 1, message = "topK 至少为 1")
        @Max(value = 100, message = "topK 最多为 100")
        private int topK = 5;

        @Min(value = 0, message = "threshold 不能为负数")
        @Max(value = 1, message = "threshold 不能超过 1")
        private float threshold = 0.7f;
    }

    @Data
    public static class KeywordSearchRequest {
        @NotBlank(message = "查询内容不能为空")
        private String query;
    }

    @Data
    public static class CompareSearchRequest {
        @NotBlank(message = "查询内容不能为空")
        private String query;

        @Min(1)
        @Max(50)
        private int topK = 10;
    }

    @Data
    public static class SimilarCaseRequest {
        @NotBlank(message = "病例描述不能为空")
        private String caseDescription;

        @Min(1)
        @Max(20)
        private int topK = 3;
    }

    @Data
    public static class ComparisonResult {
        private String query;
        private List<MedicalDocument> keywordResults;
        private int keywordCount;
        private long keywordTimeMs;
        private List<SearchResult> semanticResults;
        private int semanticCount;
        private long semanticTimeMs;
    }

    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String message;
        private long timestamp;

        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setData(data);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }

        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            response.setTimestamp(System.currentTimeMillis());
            return response;
        }
    }
}
