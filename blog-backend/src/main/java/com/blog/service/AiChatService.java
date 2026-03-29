package com.blog.service;

import com.blog.dto.ChatMessageDTO;
import com.blog.dto.ChatSessionDTO;
import com.blog.entity.AiChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务接口
 */
public interface AiChatService {
    
    /**
     * 流式对话
     */
    Flux<String> chatStream(String sessionId, String message, Long userId);
    
    /**
     * SSE 流式对话
     */
    SseEmitter chatSse(String sessionId, String message, Long userId);
    
    /**
     * 获取会话列表
     */
    List<ChatSessionDTO> getSessions(Long userId);
    
    /**
     * 获取会话消息
     */
    List<ChatMessageDTO> getMessages(String sessionId);
    
    /**
     * 删除会话
     */
    void deleteSession(String sessionId);
    
    /**
     * 重新生成回答
     */
    Flux<String> regenerate(String sessionId, Long messageId, Long userId);
}
