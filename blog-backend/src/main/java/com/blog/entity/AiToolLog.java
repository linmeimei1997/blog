package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI 工具调用日志实体
 */
@Data
public class AiToolLog {
    
    private Long id;
    
    private String sessionId;
    
    private String messageId;
    
    private String toolName;
    
    private String toolInput;
    
    private String toolOutput;
    
    private Long duration;
    
    private Integer status;
    
    private String errorMsg;
    
    private LocalDateTime createTime;
}
