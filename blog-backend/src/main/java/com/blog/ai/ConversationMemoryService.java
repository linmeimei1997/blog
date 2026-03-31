package com.blog.ai;

import com.blog.entity.AiChatMessage;
import com.blog.mapper.AiChatMessageMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 对话记忆服务 - 管理多轮对话上下文
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final AiChatMessageMapper messageMapper;

    @Value("${app.ai.memory.max-context-messages:20}")
    private int maxContextMessages;

    @Value("${app.ai.memory.summarize-threshold:50}")
    private int summarizeThreshold;

    @Value("${app.ai.memory.cache-size:1000}")
    private int cacheSize;

    // 会话摘要缓存
    private Cache<String, ConversationSummary> summaryCache;
    
    // 会话元数据
    private final Map<String, SessionMetadata> sessionMetadata = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        summaryCache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterAccess(Duration.ofHours(24))
                .build();
    }

    /**
     * 构建对话上下文
     */
    public List<Message> buildContext(String sessionId, String currentMessage, String systemPrompt) {
        List<Message> messages = new ArrayList<>();
        
        // 添加系统提示
        messages.add(new SystemMessage(systemPrompt));
        
        // 获取会话摘要（如果有）
        ConversationSummary summary = summaryCache.getIfPresent(sessionId);
        if (summary != null && summary.getContent() != null) {
            messages.add(new SystemMessage("【历史对话摘要】\n" + summary.getContent()));
        }
        
        // 获取近期消息
        List<AiChatMessage> recentMessages = getRecentMessages(sessionId);
        
        // 添加历史消息
        for (AiChatMessage msg : recentMessages) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }
        
        // 添加当前消息
        messages.add(new UserMessage(currentMessage));
        
        return messages;
    }

    /**
     * 获取近期消息
     */
    private List<AiChatMessage> getRecentMessages(String sessionId) {
        // 从数据库获取最近的消息
        List<AiChatMessage> allMessages = messageMapper.selectRecentBySessionId(sessionId, maxContextMessages);
        
        // 按时间正序排列
        return allMessages.stream()
                .sorted((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 检查是否需要生成摘要
     */
    public void checkAndSummarize(String sessionId) {
        SessionMetadata metadata = sessionMetadata.get(sessionId);
        if (metadata == null) {
            metadata = new SessionMetadata();
            sessionMetadata.put(sessionId, metadata);
        }
        
        int messageCount = messageMapper.countBySessionId(sessionId);
        
        // 当消息数超过阈值时生成摘要
        if (messageCount >= summarizeThreshold && !metadata.isSummarized()) {
            generateSummary(sessionId);
            metadata.setSummarized(true);
        }
        
        metadata.setMessageCount(messageCount);
    }

    /**
     * 生成对话摘要
     */
    private void generateSummary(String sessionId) {
        try {
            List<AiChatMessage> messages = messageMapper.selectBySessionId(sessionId);
            
            // 提取关键话题
            String summary = extractTopics(messages);
            
            ConversationSummary convSummary = new ConversationSummary();
            convSummary.setSessionId(sessionId);
            convSummary.setContent(summary);
            convSummary.setMessageCount(messages.size());
            convSummary.setGeneratedAt(System.currentTimeMillis());
            
            summaryCache.put(sessionId, convSummary);
            
            log.info("生成会话摘要: sessionId={}, messages={}", sessionId, messages.size());
        } catch (Exception e) {
            log.error("生成摘要失败: {}", e.getMessage());
        }
    }

    /**
     * 提取话题摘要
     */
    private String extractTopics(List<AiChatMessage> messages) {
        StringBuilder summary = new StringBuilder();
        summary.append("本次对话主要涉及以下话题：\n");
        
        // 简单提取用户问题作为话题
        List<String> userQuestions = messages.stream()
                .filter(m -> "user".equals(m.getRole()))
                .map(AiChatMessage::getContent)
                .filter(c -> c.length() > 5)
                .limit(5)
                .collect(Collectors.toList());
        
        for (int i = 0; i < userQuestions.size(); i++) {
            String question = userQuestions.get(i);
            // 截断过长的问题
            if (question.length() > 50) {
                question = question.substring(0, 50) + "...";
            }
            summary.append("- ").append(question).append("\n");
        }
        
        return summary.toString();
    }

    /**
     * 清除会话记忆
     */
    public void clearMemory(String sessionId) {
        summaryCache.invalidate(sessionId);
        sessionMetadata.remove(sessionId);
        log.info("清除会话记忆: {}", sessionId);
    }

    /**
     * 获取会话统计
     */
    public SessionStats getSessionStats(String sessionId) {
        SessionStats stats = new SessionStats();
        stats.setSessionId(sessionId);
        stats.setMessageCount(messageMapper.countBySessionId(sessionId));
        
        ConversationSummary summary = summaryCache.getIfPresent(sessionId);
        stats.setHasSummary(summary != null);
        if (summary != null) {
            stats.setSummaryLength(summary.getContent().length());
        }
        
        return stats;
    }

    // 内部类
    @lombok.Data
    public static class ConversationSummary {
        private String sessionId;
        private String content;
        private int messageCount;
        private long generatedAt;
    }

    @lombok.Data
    public static class SessionMetadata {
        private int messageCount;
        private boolean summarized;
        private long lastActivity;
    }

    @lombok.Data
    public static class SessionStats {
        private String sessionId;
        private int messageCount;
        private boolean hasSummary;
        private int summaryLength;
    }
}
