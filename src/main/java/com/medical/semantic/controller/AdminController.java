# 管理 API 控制器
# 用于系统管理操作，如重建索引

package com.medical.semantic.controller;

import com.medical.semantic.service.DocumentService;
import com.medical.semantic.service.VectorIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "系统管理", description = "系统管理相关接口")
public class AdminController {

    private final DocumentService documentService;
    private final VectorIndexService vectorIndexService;

    /**
     * 重建全部向量索引
     */
    @PostMapping("/rebuild-index")
    @Operation(summary = "重建向量索引", description = "清空并重建所有文档的向量索引")
    public SearchController.ApiResponse<Map<String, Object>> rebuildIndex() {
        log.info("开始重建向量索引");

        // 获取所有文档
        var allDocuments = documentService.findAll();

        // 重建索引
        vectorIndexService.rebuildAllIndexes(allDocuments);

        return SearchController.ApiResponse.success(
                Map.of(
                        "message", "索引重建完成",
                        "documentCount", allDocuments.size()
                )
        );
    }
}
