package com.blog.ai;

import com.blog.entity.AiToolLog;
import com.blog.mapper.AiToolLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tool 执行日志记录器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecutionLogger {

    private final AiToolLogMapper toolLogMapper;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 记录工具调用完成（成功）
     */
    public void logSuccess(String sessionId, String messageId, String toolName, 
                           Object input, Object output, long durationMs) {
        CompletableFuture.runAsync(() -> {
            try {
                AiToolLog toolLog = new AiToolLog();
                toolLog.setSessionId(sessionId);
                toolLog.setMessageId(messageId);
                toolLog.setToolName(toolName);
                toolLog.setToolInput(serialize(input));
                toolLog.setToolOutput(serialize(output));
                toolLog.setDuration(durationMs);
                toolLog.setStatus(1); // 成功
                toolLog.setCreateTime(LocalDateTime.now());
                
                toolLogMapper.insert(toolLog);
            } catch (Exception e) {
                log.error("记录工具调用成功日志失败: {}", e.getMessage());
            }
        }, executor);
    }

    /**
     * 记录工具调用失败
     */
    public void logError(String sessionId, String messageId, String toolName,
                         Object input, Throwable error, long durationMs) {
        CompletableFuture.runAsync(() -> {
            try {
                AiToolLog toolLog = new AiToolLog();
                toolLog.setSessionId(sessionId);
                toolLog.setMessageId(messageId);
                toolLog.setToolName(toolName);
                toolLog.setToolInput(serialize(input));
                toolLog.setDuration(durationMs);
                toolLog.setStatus(0); // 失败
                toolLog.setErrorMsg(error.getMessage());
                toolLog.setCreateTime(LocalDateTime.now());
                
                toolLogMapper.insert(toolLog);
            } catch (Exception e) {
                log.error("记录工具调用失败日志失败: {}", e.getMessage());
            }
        }, executor);
    }

    /**
     * 带日志记录的工具执行包装器
     */
    public <T> T executeWithLog(String sessionId, String messageId, String toolName, 
                                 Object input, ToolExecutor<T> executorFn) {
        long startTime = System.currentTimeMillis();
        
        try {
            T result = executorFn.execute();
            long duration = System.currentTimeMillis() - startTime;
            logSuccess(sessionId, messageId, toolName, input, result, duration);
            
            log.info("工具执行成功 [{}]: {}ms", toolName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logError(sessionId, messageId, toolName, input, e, duration);
            
            log.error("工具执行失败 [{}]: {}ms - {}", toolName, duration, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof String) return (String) obj;
            String json = objectMapper.writeValueAsString(obj);
            // 截断过长内容
            if (json.length() > 4000) {
                return json.substring(0, 4000) + "...";
            }
            return json;
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @FunctionalInterface
    public interface ToolExecutor<T> {
        T execute() throws Exception;
    }
}
