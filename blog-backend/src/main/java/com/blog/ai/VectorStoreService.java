package com.blog.ai;

import io.milvus.client.MilvusServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量存储服务 - 内存存储模式
 */
@Slf4j
@Service
public class VectorStoreService {

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Value("${app.ai.rag.vector-dim:1536}")
    private int vectorDim;

    // 内存存储
    private final Map<String, VectorDocument> memoryStore = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("向量存储服务初始化完成，使用内存存储模式");
    }

    /**
     * 添加文档到向量存储
     */
    public void addDocument(String id, List<Float> vector, String content, Long docId, Map<String, Object> metadata) {
        VectorDocument doc = new VectorDocument();
        doc.setId(id);
        doc.setVector(vector);
        doc.setContent(content);
        doc.setDocId(docId);
        doc.setMetadata(metadata);
        memoryStore.put(id, doc);
    }

    /**
     * 相似度搜索
     */
    public List<SearchResult> search(List<Float> queryVector, int topK) {
        return memoryStore.values().stream()
                .map(doc -> {
                    SearchResult sr = new SearchResult();
                    sr.setContent(doc.getContent());
                    sr.setDocId(doc.getDocId());
                    sr.setScore(cosineSimilarity(queryVector, doc.getVector()));
                    return sr;
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(topK)
                .collect(Collectors.toList());
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) return 0.0;
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }
        
        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 删除文档
     */
    public void deleteByDocId(Long docId) {
        memoryStore.values().removeIf(doc -> doc.getDocId() != null && doc.getDocId().equals(docId));
    }

    // 内部类
    @lombok.Data
    public static class VectorDocument {
        private String id;
        private List<Float> vector;
        private String content;
        private Long docId;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    public static class SearchResult {
        private String content;
        private Long docId;
        private double score;
        private Map<String, Object> metadata;
    }
}
