package com.medical.semantic.controller;

import com.medical.semantic.domain.MedicalDocument;
import com.medical.semantic.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * 文档管理 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "文档管理", description = "医学文档的增删改查操作")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @PostMapping
    @Operation(summary = "创建文档", description = "添加新的医学文档并自动生成向量索引")
    public SearchController.ApiResponse<MedicalDocument> createDocument(
            @RequestBody @Validated CreateDocumentRequest request) {

        log.info("创建文档: {}", request.getTitle());

        MedicalDocument document = MedicalDocument.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(MedicalDocument.DocumentType.valueOf(request.getType()))
                .specialty(request.getSpecialty())
                .keywords(request.getKeywords())
                .author(request.getAuthor())
                .source(request.getSource())
                .build();

        MedicalDocument created = documentService.create(document);

        return SearchController.ApiResponse.success(created);
    }

    /**
     * 批量导入文档
     */
    @PostMapping("/batch")
    @Operation(summary = "批量导入", description = "批量导入医学文档")
    public SearchController.ApiResponse<Map<String, Object>> batchImport(
            @RequestBody List<CreateDocumentRequest> requests) {

        log.info("批量导入文档, 数量: {}", requests.size());

        List<MedicalDocument> documents = requests.stream()
                .map(req -> MedicalDocument.builder()
                        .title(req.getTitle())
                        .content(req.getContent())
                        .type(MedicalDocument.DocumentType.valueOf(req.getType()))
                        .specialty(req.getSpecialty())
                        .keywords(req.getKeywords())
                        .author(req.getAuthor())
                        .source(req.getSource())
                        .build())
                .toList();

        documentService.createBatch(documents);

        return SearchController.ApiResponse.success(
                Map.of("imported", documents.size())
        );
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文档", description = "根据 ID 获取文档详情")
    public SearchController.ApiResponse<MedicalDocument> getDocument(@PathVariable Long id) {
        MedicalDocument document = documentService.findById(id);
        if (document == null) {
            return SearchController.ApiResponse.error("文档不存在");
        }
        return SearchController.ApiResponse.success(document);
    }

    /**
     * 获取所有文档
     */
    @GetMapping
    @Operation(summary = "获取所有文档", description = "查询所有文档列表")
    public SearchController.ApiResponse<List<MedicalDocument>> listDocuments(
            @RequestParam(required = false) String type) {

        List<MedicalDocument> documents;

        if (type != null) {
            documents = documentService.findByType(MedicalDocument.DocumentType.valueOf(type));
        } else {
            documents = documentService.findAll();
        }

        return SearchController.ApiResponse.success(documents);
    }

    /**
     * 更新文档
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新文档", description = "更新文档内容并重建向量索引")
    public SearchController.ApiResponse<MedicalDocument> updateDocument(
            @PathVariable Long id,
            @RequestBody @Validated UpdateDocumentRequest request) {

        log.info("更新文档: {}", id);

        MedicalDocument document = documentService.findById(id);
        if (document == null) {
            return SearchController.ApiResponse.error("文档不存在");
        }

        if (request.getTitle() != null) document.setTitle(request.getTitle());
        if (request.getContent() != null) document.setContent(request.getContent());
        if (request.getKeywords() != null) document.setKeywords(request.getKeywords());

        MedicalDocument updated = documentService.update(document);

        return SearchController.ApiResponse.success(updated);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文档", description = "删除文档及其向量索引")
    public SearchController.ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        log.info("删除文档: {}", id);
        documentService.delete(id);
        return SearchController.ApiResponse.success(null);
    }

    /**
     * 统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "统计信息", description = "获取文档库的统计信息")
    public SearchController.ApiResponse<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
                "total", documentService.countTotal(),
                "literature", documentService.countByType(MedicalDocument.DocumentType.LITERATURE),
                "case", documentService.countByType(MedicalDocument.DocumentType.CASE),
                "guideline", documentService.countByType(MedicalDocument.DocumentType.GUIDELINE)
        );
        return SearchController.ApiResponse.success(stats);
    }

    // ==================== 请求模型 ====================

    @Data
    public static class CreateDocumentRequest {
        @NotBlank(message = "标题不能为空")
        private String title;

        @NotBlank(message = "内容不能为空")
        private String content;

        @NotBlank(message = "类型不能为空")
        private String type; // LITERATURE, CASE, GUIDELINE

        private String specialty;
        private String keywords;
        private String author;
        private String source;
    }

    @Data
    public static class UpdateDocumentRequest {
        private String title;
        private String content;
        private String keywords;
    }
}
