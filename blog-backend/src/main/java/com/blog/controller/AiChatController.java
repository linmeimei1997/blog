package com.blog.controller;

import com.blog.dto.ChatMessageDTO;
import com.blog.dto.ChatRequest;
import com.blog.dto.ChatSessionDTO;
import com.blog.dto.Result;
import com.blog.service.AiChatService;
import com.blog.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * SSE 流式对话
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId = 1L; // 默认用户
        }
        
        return aiChatService.chatSse(sessionId, message, userId);
    }

    /**
     * 发送消息
     */
    @PostMapping("/chat/send")
    public Result<String> sendMessage(@RequestBody ChatRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId = 1L;
        }
        
        // 非流式响应，简化处理
        return Result.success("消息已接收");
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<List<ChatSessionDTO>> getSessions() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId = 1L;
        }
        List<ChatSessionDTO> sessions = aiChatService.getSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 获取会话消息
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ChatMessageDTO>> getMessages(@PathVariable String sessionId) {
        List<ChatMessageDTO> messages = aiChatService.getMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        aiChatService.deleteSession(sessionId);
        return Result.success();
    }
    
    /**
     * 上传图片（用于图文读书笔记）
     */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        log.info("收到图片上传请求, 文件名: {}, 大小: {}", 
                file.getOriginalFilename(), file.getSize());
        
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            userId = 1L;
        }
        
        try {
            // 保存图片到上传目录
            String imageUrl = saveImage(file);
            log.info("图片上传成功, URL: {}", imageUrl);
            return Result.success(imageUrl);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }
    
    private String saveImage(MultipartFile file) throws Exception {
        // 使用项目目录下的绝对路径
        String projectDir = System.getProperty("user.dir");
        String uploadDir = projectDir + java.io.File.separator + "uploads" + java.io.File.separator + "images";
        java.io.File dir = new java.io.File(uploadDir);
        
        log.info("图片上传目录: {}", uploadDir);
        
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new RuntimeException("无法创建上传目录: " + uploadDir);
            }
            log.info("创建上传目录成功");
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = java.util.UUID.randomUUID().toString() + extension;
        java.io.File dest = new java.io.File(dir, fileName);
        
        log.info("保存图片到: {}", dest.getAbsolutePath());
        
        file.transferTo(dest);
        
        // 返回相对路径，前端会通过代理访问
        return "/uploads/images/" + fileName;
    }
}
