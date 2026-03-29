package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 对话消息实体
 */
@Data
public class AiChatMessage {
    
    private Long id;
    
    private String sessionId;
    
    private Long userId;
    
    private String role;
    
    private String content;
    
    private String messageType;
    
    private String toolsUsed;
    
    private Integer tokens;
    
    private LocalDateTime createTime;
}
