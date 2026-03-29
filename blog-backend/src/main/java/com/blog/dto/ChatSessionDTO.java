package com.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话会话DTO
 */
@Data
public class ChatSessionDTO {
    
    private String sessionId;
    private String title;
    private Integer messageCount;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createTime;
}
