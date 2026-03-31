package com.blog.ai;

import com.blog.entity.KbChunk;
import com.blog.entity.KbDocument;
import com.blog.mapper.KbChunkMapper;
import com.blog.mapper.KbDocumentMapper;
import com.blog.ai.VectorStoreService.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务 - 增强版
 * 支持向量检索、混合检索、来源引用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KbChunkMapper chunkMapper;
    private final KbDocumentMapper documentMapper;
    private final VectorStoreService vectorStore;
    private final EmbeddingService embeddingService;

    @Value("${app.ai.rag.top-k:5}")
    private int topK;

    @Value("${app.ai.rag.score-threshold:0.5}")
    private double scoreThreshold;

    @Value("${app.ai.rag.enable-vector:true}")
    private boolean enableVectorSearch;

    @Value("${app.ai.rag.enable-hybrid:true}")
    private boolean enableHybridSearch;

    /**
     * 检索相关上下文（带来源引用）
     */
    public RetrievalResult retrieveContext(String query) {
        log.debug("RAG检索查询: {}", query);
        
        List<RetrievalItem> items;
        
        if (enableHybridSearch) {
            // 混合检索：向量 + 关键词
            items = hybridSearch(query);
        } else if (enableVectorSearch) {
            // 纯向量检索
            items = vectorSearch(query);
        } else {
            // 纯关键词检索
            items = keywordSearch(query);
        }
        
        if (items.isEmpty()) {
            return RetrievalResult.empty();
        }
        
        // 构建上下文
        String context = items.stream()
                .map(item -> String.format("【来源: %s】\n%s", item.getSource(), item.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
        
        RetrievalResult result = new RetrievalResult();
        result.setContext(context);
        result.setItems(items);
        result.setTotalItems(items.size());
        
        log.debug("RAG检索完成，找到 {} 个相关片段", items.size());
        return result;
    }

    /**
     * 向量检索
     */
    private List<RetrievalItem> vectorSearch(String query) {
        try {
            List<Float> queryVector = embeddingService.embed(query);
            List<SearchResult> results = vectorStore.search(queryVector, topK * 2);
            
            return results.stream()
                    .filter(r -> r.getScore() >= scoreThreshold)
                    .limit(topK)
                    .map(this::convertToRetrievalItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 关键词检索
     */
    private List<RetrievalItem> keywordSearch(String query) {
        List<KbChunk> chunks = chunkMapper.searchByKeyword(query);
        
        return chunks.stream()
                .limit(topK)
                .map(this::convertToRetrievalItem)
                .collect(Collectors.toList());
    }

    /**
     * 混合检索
     */
    private List<RetrievalItem> hybridSearch(String query) {
        // 获取向量检索结果
        List<RetrievalItem> vectorResults = vectorSearch(query);
        
        // 获取关键词检索结果
        List<RetrievalItem> keywordResults = keywordSearch(query);
        
        // 融合排序（RRF算法）
        return reciprocalRankFusion(vectorResults, keywordResults);
    }

    /**
     * RRF 融合排序
     */
    private List<RetrievalItem> reciprocalRankFusion(
            List<RetrievalItem> vectorResults, 
            List<RetrievalItem> keywordResults) {
        
        Map<String, Double> scores = new HashMap<>();
        Map<String, RetrievalItem> itemMap = new HashMap<>();
        
        int k = 60; // RRF常数
        
        // 向量检索分数
        for (int i = 0; i < vectorResults.size(); i++) {
            String key = vectorResults.get(i).getChunkId();
            scores.merge(key, 1.0 / (k + i + 1), Double::sum);
            itemMap.put(key, vectorResults.get(i));
        }
        
        // 关键词检索分数
        for (int i = 0; i < keywordResults.size(); i++) {
            String key = keywordResults.get(i).getChunkId();
            scores.merge(key, 1.0 / (k + i + 1), Double::sum);
            if (!itemMap.containsKey(key)) {
                itemMap.put(key, keywordResults.get(i));
            }
        }
        
        // 按分数排序
        return scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> {
                    RetrievalItem item = itemMap.get(e.getKey());
                    item.setHybridScore(e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检索文档
     */
    public List<RetrievalItem> searchDocuments(String query) {
        RetrievalResult result = retrieveContext(query);
        return result.getItems();
    }

    /**
     * 总结文档
     */
    public String summarizeDocument(Long documentId) {
        List<KbChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return "文档为空或不存在";
        }
        
        String content = chunks.stream()
                .map(KbChunk::getContent)
                .collect(Collectors.joining("\n"));
        
        // 截取前2000字符作为摘要
        if (content.length() > 2000) {
            return content.substring(0, 2000) + "...";
        }
        return content;
    }

    /**
     * 获取文档来源信息
     */
    private RetrievalItem convertToRetrievalItem(KbChunk chunk) {
        RetrievalItem item = new RetrievalItem();
        item.setChunkId(String.valueOf(chunk.getId()));
        item.setContent(chunk.getContent());
        item.setDocId(chunk.getDocumentId());
        
        // 获取文档信息
        KbDocument doc = documentMapper.selectById(chunk.getDocumentId());
        if (doc != null) {
            item.setSource(doc.getTitle());
            item.setDocType(doc.getFileType());
        } else {
            item.setSource("未知来源");
        }
        
        item.setScore(1.0); // 关键词检索默认分数
        return item;
    }

    private RetrievalItem convertToRetrievalItem(SearchResult result) {
        RetrievalItem item = new RetrievalItem();
        item.setChunkId("vec_" + result.getDocId());
        item.setContent(result.getContent());
        item.setDocId(result.getDocId());
        item.setScore(result.getScore());
        
        // 获取文档信息
        KbDocument doc = documentMapper.selectById(result.getDocId());
        if (doc != null) {
            item.setSource(doc.getTitle());
            item.setDocType(doc.getFileType());
        } else {
            item.setSource("向量检索结果");
        }
        
        return item;
    }

    // 内部类
    @lombok.Data
    public static class RetrievalResult {
        private String context;
        private List<RetrievalItem> items;
        private int totalItems;
        private long retrievalTime;
        
        public static RetrievalResult empty() {
            RetrievalResult result = new RetrievalResult();
            result.setContext("");
            result.setItems(new ArrayList<>());
            result.setTotalItems(0);
            return result;
        }
    }

    @lombok.Data
    public static class RetrievalItem {
        private String chunkId;
        private String content;
        private Long docId;
        private String source;
        private String docType;
        private double score;
        private double hybridScore;
        private Map<String, Object> metadata;
    }
}
