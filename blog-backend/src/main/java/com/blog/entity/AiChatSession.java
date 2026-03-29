package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 对话会话实体
 */
@Data
public class AiChatSession {
    
    private Long id;
    
    private String sessionId;
    
    private Long userId;
    
    private String title;
    
    private Integer messageCount;
    
    private LocalDateTime lastMessageTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
