package com.blog.ai;

import com.blog.entity.AiChatMessage;
import com.blog.mapper.AiChatMessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 可观测性服务 - 记录 AI 调用全链路日志
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiObservabilityService {

    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 记录 AI 调用日志
     */
    public void logAiCall(AiCallLog logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                // 异步记录到日志系统
                log.info("[AI-CALL] session={} duration={}ms tokens={} status={}",
                        logEntry.getSessionId(),
                        logEntry.getDurationMs(),
                        logEntry.getTokensUsed(),
                        logEntry.getStatus());
                
                // 详细日志
                if (log.isDebugEnabled()) {
                    log.debug("[AI-CALL-DETAIL] {}", objectMapper.writeValueAsString(logEntry));
                }
            } catch (Exception e) {
                log.error("记录AI调用日志失败", e);
            }
        }, executor);
    }

    /**
     * 记录 RAG 检索日志
     */
    public void logRagRetrieval(RagRetrievalLog logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[RAG-RETRIEVAL] query='{}' results={} duration={}ms",
                        truncate(logEntry.getQuery(), 50),
                        logEntry.getResultsCount(),
                        logEntry.getDurationMs());
            } catch (Exception e) {
                log.error("记录RAG检索日志失败", e);
            }
        }, executor);
    }

    /**
     * 记录工具调用日志
     */
    public void logToolCall(ToolCallLog logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[TOOL-CALL] tool={} duration={}ms status={}",
                        logEntry.getToolName(),
                        logEntry.getDurationMs(),
                        logEntry.getStatus());
            } catch (Exception e) {
                log.error("记录工具调用日志失败", e);
            }
        }, executor);
    }

    /**
     * 记录性能指标
     */
    public void logPerformance(PerformanceMetrics metrics) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[PERFORMANCE] latency={}ms throughput={} errors={}",
                        metrics.getAvgLatency(),
                        metrics.getThroughput(),
                        metrics.getErrorRate());
            } catch (Exception e) {
                log.error("记录性能指标失败", e);
            }
        }, executor);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    // 日志实体类
    @lombok.Data
    public static class AiCallLog {
        private String logId;
        private String sessionId;
        private String messageId;
        private String model;
        private String prompt;
        private String response;
        private int tokensUsed;
        private long durationMs;
        private String status; // success, error, timeout
        private String errorMsg;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    public static class RagRetrievalLog {
        private String logId;
        private String sessionId;
        private String query;
        private int resultsCount;
        private long durationMs;
        private String strategy; // vector, keyword, hybrid
        private double avgScore;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    public static class ToolCallLog {
        private String logId;
        private String sessionId;
        private String messageId;
        private String toolName;
        private String input;
        private String output;
        private long durationMs;
        private String status;
        private String errorMsg;
        private LocalDateTime timestamp;
    }

    @lombok.Data
    public static class PerformanceMetrics {
        private long avgLatency;
        private double throughput;
        private double errorRate;
        private int totalRequests;
        private int errorRequests;
        private LocalDateTime timestamp;
    }
}
