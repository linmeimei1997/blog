package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

/**
 * AI 对话请求
 */
@Data
public class ChatRequest {
    
    private String sessionId;
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    private Boolean stream = true;
    
    private List<String> context;
}
