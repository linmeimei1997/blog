package com.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 对话消息DTO
 */
@Data
public class ChatMessageDTO {
    
    private Long id;
    private String role;
    private String content;
    private String messageType;
    private LocalDateTime createTime;
}
