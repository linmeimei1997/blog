package com.blog.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;

/**
 * 文本向量化服务 - 支持多种嵌入模型
 */
@Slf4j
@Service
public class EmbeddingService {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${app.ai.embedding.provider:dashscope}")
    private String provider;

    @Value("${app.ai.embedding.model:text-embedding-v2}")
    private String model;

    @Value("${app.ai.embedding.dim:1536}")
    private int embeddingDim;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // 本地缓存
    private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();

    /**
     * 获取文本向量
     */
    public List<Float> embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return createZeroVector();
        }

        // 检查缓存
        String cacheKey = text.hashCode() + "_" + text.length();
        if (embeddingCache.containsKey(cacheKey)) {
            return embeddingCache.get(cacheKey);
        }

        try {
            List<Float> embedding = callEmbeddingApi(text);
            embeddingCache.put(cacheKey, embedding);
            return embedding;
        } catch (Exception e) {
            log.error("文本向量化失败: {}", e.getMessage());
            return createZeroVector();
        }
    }

    /**
     * 批量获取文本向量
     */
    public List<List<Float>> embedBatch(List<String> texts) {
        List<Future<List<Float>>> futures = new ArrayList<>();
        
        for (String text : texts) {
            futures.add(executor.submit(() -> embed(text)));
        }

        List<List<Float>> results = new ArrayList<>();
        for (Future<List<Float>> future : futures) {
            try {
                results.add(future.get(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.error("批量向量化失败: {}", e.getMessage());
                results.add(createZeroVector());
            }
        }
        return results;
    }

    /**
     * 调用嵌入API
     */
    private List<Float> callEmbeddingApi(String text) {
        return switch (provider.toLowerCase()) {
            case "dashscope" -> callDashscopeApi(text);
            case "openai" -> callOpenAiApi(text);
            default -> createRandomVector(text);
        };
    }

    /**
     * 调用 Dashscope 嵌入 API
     */
    private List<Float> callDashscopeApi(String text) {
        try {
            String url = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", Map.of("texts", Collections.singletonList(text)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("output")) {
                Map<String, Object> output = (Map<String, Object>) response.getBody().get("output");
                List<Map<String, Object>> embeddings = (List<Map<String, Object>>) output.get("embeddings");
                if (!embeddings.isEmpty()) {
                    List<Double> embedding = (List<Double>) embeddings.get(0).get("embedding");
                    return embedding.stream().map(Double::floatValue).toList();
                }
            }
        } catch (Exception e) {
            log.error("Dashscope API 调用失败: {}", e.getMessage());
        }
        return createRandomVector(text);
    }

    /**
     * 调用 OpenAI 嵌入 API (兼容格式)
     */
    private List<Float> callOpenAiApi(String text) {
        // 预留 OpenAI 兼容接口
        return createRandomVector(text);
    }

    /**
     * 创建零向量
     */
    private List<Float> createZeroVector() {
        List<Float> vector = new ArrayList<>(embeddingDim);
        for (int i = 0; i < embeddingDim; i++) {
            vector.add(0.0f);
        }
        return vector;
    }

    /**
     * 创建随机向量（降级方案）
     */
    private List<Float> createRandomVector(String inputText) {
        Random random = new Random(inputText != null ? inputText.hashCode() : System.currentTimeMillis());
        List<Float> vector = new ArrayList<>(embeddingDim);
        for (int i = 0; i < embeddingDim; i++) {
            vector.add(random.nextFloat() * 2 - 1);
        }
        // 归一化
        double norm = Math.sqrt(vector.stream().mapToDouble(v -> v * v).sum());
        if (norm > 0) {
            final double finalNorm = norm;
            return vector.stream().map(v -> (float) (v / finalNorm)).toList();
        }
        return vector;
    }

    /**
     * 计算向量维度
     */
    public int getDimension() {
        return embeddingDim;
    }
}
