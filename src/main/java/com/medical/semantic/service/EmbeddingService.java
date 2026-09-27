package com.medical.semantic.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Embedding 服务 - 将文本转换为向量
 *
 * 支持两种模式:
 * 1. 本地模式: 使用本地部署的 BGE-M3 模型
 * 2. 云端模式: 调用 MiniMax Embedding API
 */
@Slf4j
@Service
public class EmbeddingService {

    @Value("${embedding.provider}")
    private String provider;

    @Value("${embedding.api.url}")
    private String apiUrl;

    @Value("${embedding.api.key}")
    private String apiKey;

    @Value("${embedding.model}")
    private String model;

    private final WebClient webClient;

    public EmbeddingService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 生成文本的向量表示
     *
     * @param text 输入文本
     * @return 768 维向量
     */
    public List<Float> embed(String text) {
        if ("local".equals(provider)) {
            return embedLocal(text);
        } else {
            return embedCloud(text);
        }
    }

    /**
     * 批量生成向量
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        log.info("批量生成 embedding, 数量: {}", texts.size());
        return texts.stream()
                .map(this::embed)
                .toList();
    }

    /**
     * 本地模式: 使用本地 BGE-M3 模型
     *
     * 注意: 这里提供模拟实现用于演示和测试
     * 生产环境应该调用实际的本地模型服务（FastAPI + BGE-M3）
     */
    private List<Float> embedLocal(String text) {
        log.debug("使用本地模型生成 embedding: {}", model);
        log.warn("当前使用模拟 embedding，仅用于演示！生产环境请部署真实的 BGE-M3 模型服务");

        // 模拟 embedding: 基于文本哈希生成确定性的 768 维向量
        // 这样相同的文本总是得到相同的向量，保证检索的一致性
        return generateMockEmbedding(text, 768);
    }

    /**
     * 生成模拟的 Embedding 向量
     *
     * 说明: 这是一个简化的实现，用于演示和测试
     * 真实场景应该调用实际的 Embedding 模型
     *
     * @param text 输入文本
     * @param dimension 向量维度
     * @return 模拟的 embedding 向量
     */
    private List<Float> generateMockEmbedding(String text, int dimension) {
        // 使用文本的哈希码作为随机种子，保证相同文本得到相同向量
        int seed = text.hashCode();
        java.util.Random random = new java.util.Random(seed);

        // 生成随机向量
        List<Float> vector = new java.util.ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add((float) random.nextGaussian());
        }

        // 归一化向量（用于余弦相似度计算）
        float norm = 0.0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < dimension; i++) {
            vector.set(i, vector.get(i) / norm);
        }

        return vector;
    }

    /**
     * 云端模式: 调用 MiniMax Embedding API
     */
    private List<Float> embedCloud(String text) {
        log.debug("调用云端 API 生成 embedding");

        try {
            Map<String, Object> requestBody = Map.of(
                "model", model,
                "texts", List.of(text),
                "type", "query"  // MiniMax 支持 query 和 db 两种类型
            );

            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Embedding API 返回为空");
            }

            // 解析响应
            List<Map<String, Object>> vectors = (List<Map<String, Object>>) response.get("vectors");
            if (vectors == null || vectors.isEmpty()) {
                throw new RuntimeException("Embedding API 未返回向量");
            }

            List<Double> embedding = (List<Double>) vectors.get(0).get("embedding");

            // 转换为 Float 列表
            return embedding.stream()
                    .map(Double::floatValue)
                    .toList();

        } catch (Exception e) {
            log.error("调用 Embedding API 失败", e);
            throw new RuntimeException("生成 embedding 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 相似度 [0, 1]
     */
    public float cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
