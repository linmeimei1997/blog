package com.blog.ai;

import com.blog.dto.ChatMessageDTO;
import com.blog.dto.ChatSessionDTO;
import com.blog.entity.AiChatMessage;
import com.blog.entity.AiChatSession;
import com.blog.entity.Article;
import com.blog.entity.ArticleImage;
import com.blog.mapper.AiChatMessageMapper;
import com.blog.mapper.AiChatSessionMapper;
import com.blog.service.AiChatService;
import com.blog.ai.RagService.RetrievalResult;
import com.blog.ai.ToolCallingService.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 对话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final org.springframework.ai.chat.model.ChatModel chatModel;
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final RagService ragService;
    private final ToolCallingService toolCallingService;
    private final ConversationMemoryService memoryService;
    private final com.blog.service.ArticleService articleService;
    private final com.blog.service.KbDocumentService kbDocumentService;
    private final com.blog.mapper.ArticleImageMapper articleImageMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个智能AI助手，可以帮助用户：
            1. 回答关于博客文章的问题
            2. 搜索和总结知识库文档
            3. 创建博客文章（系统会自动保存到博客和知识库）
            4. 写读书笔记和感悟
            5. 管理文章：搜索、修改、删除已有的笔记和博客
            6. 解析网页：根据用户提供的URL解析网页内容，并进行分析总结
            
            【重要概念】：
            - "笔记"和"文章"在系统中是同一概念，都存储在文章表中
            - 用户说"笔记"时，指的就是文章
            - 用户说"删除笔记"就是要删除文章
            
            当用户要求你写博客、写文章时，请直接生成完整的文章内容，包含：
            - 标题（使用 # 开头）
            - 正文内容（使用 Markdown 格式）
            - 适当的章节划分
            
            当用户要求写读书笔记、读后感时：
            - 先检索知识库中的相关文档内容
            - 基于文档内容写出深刻的读书感悟
            - 包含：书籍/文档概述、核心观点、个人感悟、实践应用等部分
            
            【文章管理功能】：
            - 搜索文章：当用户要搜索/查找文章或笔记时，告诉我搜索关键词
            - 修改文章：当用户要修改/编辑文章或笔记时，需要提供文章ID和新内容
            - 删除文章：可以直接提供文章ID，也可以提供标题关键字
              - 如果提供ID，直接删除对应文章
              - 如果提供关键字，系统会搜索标题包含该关键字的文章
              - 如果只找到一篇匹配，直接删除
              - 如果找到多篇匹配，会列出文章让用户选择
            - 我会在后台自动执行这些操作并返回结果
            
            【网页解析功能 - 非常重要】：
            - 当用户提供URL时，系统会进行URL结构静态分析（不访问外部网站）
            - 分析结果包含：协议、域名、路径、资源类型、网站类型等信息
            - 你需要基于URL结构分析帮助用户理解链接指向的资源类型
            - 重要约束：你无法访问网页实际内容，如需分析具体内容请让用户复制粘贴
            - 你可以根据URL结构和域名推断网站类型（如技术社区、新闻媒体、电商平台等）
            - 使用场景：判断链接类型、识别网站归属、推测内容类别等
            
            【重要图片规则】：
            - 只有当用户消息中明确包含图片链接（Markdown格式的图片语法 ![...](...)）时，才在文章中保留和引用这些图片
            - 如果用户没有提供图片，不要在文章中插入任何图片占位符或图片描述
            - 不要生成虚假的图片链接或占位符
            
            请用中文回答，保持友好和专业的语气。
            """;

    @Override
    public Flux<String> chatStream(String sessionId, String message, Long userId) {
        // 创建或获取会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession(userId, message);
        }
        
        // 保存用户消息
        saveMessage(sessionId, userId, "user", message);
        
        // 构建上下文
        List<Message> messages = buildMessages(sessionId, message);
        
        // 调用 AI
        final String finalSessionId = sessionId;
        return chatModel.stream(new Prompt(messages))
                .map(response -> response.getResult().getOutput().getText())
                .doOnNext(content -> log.debug("AI response chunk: {}", content))
                .doOnComplete(() -> {
                    // 保存完整回复
                    String fullResponse = ""; // 需要在实际使用时收集
                    saveMessage(finalSessionId, null, "assistant", fullResponse);
                    sessionMapper.updateMessageCount(finalSessionId);
                });
    }

    @Override
    public SseEmitter chatSse(String sessionId, String message, Long userId) {
        SseEmitter emitter = new SseEmitter(300000L);
        
        // 检查 session 是否存在
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession(userId, message);
        } else {
            AiChatSession existingSession = sessionMapper.selectBySessionId(sessionId);
            if (existingSession == null) {
                log.warn("Session不存在，创建新session: {}", sessionId);
                sessionId = createSession(userId, message);
            }
        }
        
        final String finalSessionId = sessionId;
        final String messageId = UUID.randomUUID().toString();
        
        // 保存用户消息
        saveMessage(sessionId, userId, "user", message);
        
        // 模拟模式
        if (chatModel == null) {
            return mockResponse(emitter, finalSessionId, message, userId);
        }
        
        try {
            // 执行工具调用（带日志）
            ToolExecutionResult toolResult = toolCallingService.executeTools(finalSessionId, messageId, message);
            String toolContext = toolResult.formatForPrompt();
            
            // 使用记忆服务构建上下文
            List<Message> messages = memoryService.buildContext(sessionId, message, SYSTEM_PROMPT);
            
            // 添加工具结果
            if (!toolContext.isEmpty()) {
                messages.add(messages.size() - 1, new SystemMessage("\n【系统工具执行结果】\n" + toolContext));
            }
            
            // RAG 检索（增强版，带来源引用）
            RetrievalResult ragResult = ragService.retrieveContext(message);
            if (ragResult != null && !ragResult.getContext().isEmpty()) {
                StringBuilder ragContext = new StringBuilder("\n【知识库检索结果】\n");
                ragContext.append(ragResult.getContext());
                if (!ragResult.getItems().isEmpty()) {
                    ragContext.append("\n\n【来源引用】\n");
                    ragResult.getItems().forEach(item -> 
                        ragContext.append("- ").append(item.getSource()).append("\n"));
                }
                messages.add(1, new SystemMessage(ragContext.toString()));
            }
            
            StringBuilder fullResponse = new StringBuilder();
            
            chatModel.stream(new Prompt(messages))
                    .subscribe(
                            response -> {
                                String content = response.getResult().getOutput().getText();
                                fullResponse.append(content);
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("message")
                                            .data(content));
                                } catch (IOException e) {
                                    log.error("SSE发送失败", e);
                                }
                            },
                            error -> {
                                log.error("AI调用失败", error);
                                // 失败时返回模拟响应
                                mockResponse(emitter, finalSessionId, message, userId);
                            },
                            () -> {
                                try {
                                    String responseText = fullResponse.toString();
                                    
                                    // 检查是否是写作请求，如果是则自动保存文章
                                    String saveResult = autoSaveArticleIfNeeded(message, responseText, userId);
                                    if (saveResult != null) {
                                        // 发送保存结果
                                        emitter.send(SseEmitter.event()
                                                .name("message")
                                                .data("\n\n---\n" + saveResult));
                                        responseText = responseText + "\n\n---\n" + saveResult;
                                    }
                                    
                                    // 发送完成事件
                                    emitter.send(SseEmitter.event()
                                            .name("complete")
                                            .data("done"));
                                    // 保存完整回复
                                    saveMessage(finalSessionId, null, "assistant", responseText);
                                    sessionMapper.updateMessageCount(finalSessionId);
                                                                
                                    // 检查是否需要生成摘要
                                    memoryService.checkAndSummarize(finalSessionId);
                                                                
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("发送完成事件失败", e);
                                    emitter.completeWithError(e);
                                }
                            }
                    );
        } catch (Exception e) {
            log.error("AI服务异常", e);
            return mockResponse(emitter, finalSessionId, message, userId);
        }
        
        return emitter;
    }
    
    /**
     * 模拟响应（用于测试或AI未配置时）
     */
    private SseEmitter mockResponse(SseEmitter emitter, String sessionId, String message, Long userId) {
        String lowerMsg = message.toLowerCase();
        boolean isBookNote = lowerMsg.contains("读书") || lowerMsg.contains("读后感") || 
                lowerMsg.contains("笔记") || lowerMsg.contains("感悟");
        boolean isWriteArticle = lowerMsg.contains("写") && 
                (lowerMsg.contains("博客") || lowerMsg.contains("文章"));
        
        String response;
        if (isBookNote) {
            response = "# 《示例书籍》读书笔记\n\n" +
                    "## 书籍概述\n\n" +
                    "这是一本关于个人成长与自我提升的经典著作...\n\n" +
                    "## 核心观点\n\n" +
                    "1. **持续学习的重要性** - 在快速变化的时代，终身学习是保持竞争力的关键\n" +
                    "2. **实践出真知** - 理论知识需要通过实践来验证和深化\n" +
                    "3. **反思与总结** - 定期回顾和反思是进步的重要环节\n\n" +
                    "## 个人感悟\n\n" +
                    "读完这本书，我深刻认识到...\n\n" +
                    "## 实践应用\n\n" +
                    "我计划在日常生活中应用书中的理念...\n\n" +
                    "（这是模拟的读书笔记，实际使用时请配置 AI 服务）";
        } else if (isWriteArticle) {
            response = "# AI 技术发展趋势\n\n" +
                    "## 引言\n\n" +
                    "人工智能正在深刻改变我们的生活和工作方式...\n\n" +
                    "## 主要内容\n\n" +
                    "### 1. 大语言模型的崛起\n\n" +
                    "近年来，大语言模型如 GPT、Claude 等展现出强大的能力...\n\n" +
                    "### 2. 应用场景扩展\n\n" +
                    "从文本生成到代码编写，从图像创作到视频制作...\n\n" +
                    "## 结语\n\n" +
                    "AI 技术的发展势不可挡...\n\n" +
                    "（这是模拟的文章，实际使用时请配置 AI 服务）";
        } else {
            response = "你好！我是AI助手。\n\n你发送的消息是：" + message + 
                    "\n\n（当前AI服务未配置或调用失败，这是模拟响应。请检查：\n" +
                    "1. application.yml 中是否配置了 spring.ai.dashscope.api-key\n" +
                    "2. 网络连接是否正常）";
        }
        
        StringBuilder fullResponse = new StringBuilder();
        
        // 模拟流式输出
        new Thread(() -> {
            try {
                // 先发送连接成功事件
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(""));
                
                String[] chars = response.split("");
                for (String c : chars) {
                    fullResponse.append(c);
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(c));
                    Thread.sleep(30); // 模拟打字效果
                }
                
                // 检查是否需要自动保存（写作请求）
                String saveResult = autoSaveArticleIfNeeded(message, response, userId);
                if (saveResult != null) {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data("\n\n---\n" + saveResult));
                    fullResponse.append("\n\n---\n").append(saveResult);
                }
                
                // 发送完成事件
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data("done"));
                
                // 保存完整回复
                saveMessage(sessionId, null, "assistant", fullResponse.toString());
                sessionMapper.updateMessageCount(sessionId);
                emitter.complete();
            } catch (Exception e) {
                log.error("模拟响应失败", e);
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    @Override
    public List<ChatSessionDTO> getSessions(Long userId) {
        List<AiChatSession> sessions = sessionMapper.selectByUserId(userId);
        return sessions.stream().map(s -> {
            ChatSessionDTO dto = new ChatSessionDTO();
            dto.setSessionId(s.getSessionId());
            dto.setTitle(s.getTitle());
            dto.setMessageCount(s.getMessageCount());
            dto.setLastMessageTime(s.getLastMessageTime());
            dto.setCreateTime(s.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageDTO> getMessages(String sessionId) {
        List<AiChatMessage> messages = messageMapper.selectBySessionId(sessionId);
        return messages.stream().map(m -> {
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setId(m.getId());
            dto.setRole(m.getRole());
            dto.setContent(m.getContent());
            dto.setMessageType(m.getMessageType());
            dto.setCreateTime(m.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteSession(String sessionId) {
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteBySessionId(sessionId);
        memoryService.clearMemory(sessionId);
    }

    @Override
    public Flux<String> regenerate(String sessionId, Long messageId, Long userId) {
        // 获取该消息之前的所有消息
        List<AiChatMessage> allMessages = messageMapper.selectBySessionId(sessionId);
        List<AiChatMessage> history = allMessages.stream()
                .filter(m -> m.getId() < messageId)
                .collect(Collectors.toList());
        
        // 删除当前及之后的消息
        // 这里简化处理，实际应该标记为删除
        
        // 重新生成
        String lastUserMessage = history.stream()
                .filter(m -> "user".equals(m.getRole()))
                .reduce((first, second) -> second)
                .map(AiChatMessage::getContent)
                .orElse("");
        
        return chatStream(sessionId, lastUserMessage, userId);
    }

    private String createSession(Long userId, String firstMessage) {
        String sessionId = UUID.randomUUID().toString();
        AiChatSession session = new AiChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage);
        sessionMapper.insert(session);
        return sessionId;
    }

    private void saveMessage(String sessionId, Long userId, String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageType("text");
        messageMapper.insert(message);
    }

    /**
     * 已废弃，使用 ConversationMemoryService.buildContext
     */
    @Deprecated
    private List<Message> buildMessages(String sessionId, String currentMessage) {
        return memoryService.buildContext(sessionId, currentMessage, SYSTEM_PROMPT);
    }
    
    /**
     * 自动保存文章（如果检测到写作请求）
     */
    private String autoSaveArticleIfNeeded(String userMessage, String aiResponse, Long userId) {
        String lowerMsg = userMessage.toLowerCase();
        
        // 排除管理类请求（删除、修改、搜索、查看等）
        boolean isManageRequest = lowerMsg.contains("删除") || lowerMsg.contains("移除") ||
                lowerMsg.contains("修改") || lowerMsg.contains("编辑") || lowerMsg.contains("更新") ||
                lowerMsg.contains("搜索") || lowerMsg.contains("查找") ||
                lowerMsg.contains("查看") || lowerMsg.contains("获取") || lowerMsg.contains("列出") ||
                lowerMsg.contains("列表");
        
        if (isManageRequest) {
            return null; // 管理类请求不触发自动保存
        }
        
        // 检测写作请求类型
        boolean isWriteRequest = lowerMsg.contains("写") &&
                (lowerMsg.contains("博客") || lowerMsg.contains("文章") ||
                 lowerMsg.contains("blog") || lowerMsg.contains("article")) ||
                lowerMsg.contains("生成") && lowerMsg.contains("文章") ||
                lowerMsg.contains("创建") && lowerMsg.contains("博客");
        
        // 检测读书笔记请求
        boolean isBookNoteRequest = (lowerMsg.contains("读书") ||
                lowerMsg.contains("读后感") ||
                lowerMsg.contains("写笔记") || lowerMsg.contains("写一篇笔记") ||
                lowerMsg.contains("感悟") ||
                lowerMsg.contains("心得")) && !lowerMsg.contains("删除");

        if (!isWriteRequest && !isBookNoteRequest) {
            return null;
        }
        
        try {
            // 从 AI 响应中提取标题和内容
            String title = extractTitle(aiResponse);
            String content = aiResponse;
            String summary = extractSummary(aiResponse);
            
            if (title == null || title.isEmpty()) {
                if (isBookNoteRequest) {
                    title = "读书笔记 - " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    title = "AI生成的文章 - " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
            }
            
            // 如果是读书笔记，修改分类
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary);
            article.setAuthorId(userId);
            article.setCategoryId(isBookNoteRequest ? 3L : 1L); // 3=读书笔记分类
            article.setStatus(1);
            article.setViewCount(0);
            article.setLikeCount(0);
            
            articleService.save(article);
            
            // 提取并保存图片
            List<ArticleImage> images = extractImages(content, article.getId());
            if (!images.isEmpty()) {
                articleImageMapper.insertBatch(images);
                log.info("保存文章图片: articleId={}, count={}", article.getId(), images.size());
            }
            
            // 同时保存到知识库
            kbDocumentService.saveAsDocument(title, content, userId);
            
            log.info("AI自动保存文章: userId={}, title={}, type={}, images={}", userId, title, 
                    isBookNoteRequest ? "读书笔记" : "博客文章", images.size());
            
            String typeName = isBookNoteRequest ? "读书笔记" : "文章";
            String imageInfo = images.isEmpty() ? "" : "（包含 " + images.size() + " 张图片）";
            return "✅ " + typeName + "创建成功！标题：《" + title + "》，ID：" + article.getId() + imageInfo +
                   "\n已同时保存到知识库，可用于后续检索。";
        } catch (Exception e) {
            log.error("自动保存文章失败", e);
            return "⚠️ 内容生成成功，但保存失败: " + e.getMessage();
        }
    }
    
    /**
     * 从 Markdown 内容中提取标题
     */
    private String extractTitle(String content) {
        // 匹配 # 开头的标题
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^#\\s*(.+)$", java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 如果没有 # 标题，取第一行
        String firstLine = content.split("\\n")[0].trim();
        return firstLine.length() > 50 ? firstLine.substring(0, 50) + "..." : firstLine;
    }
    
    /**
     * 提取摘要（前200字符）
     */
    private String extractSummary(String content) {
        // 移除 Markdown 标记
        String plainText = content.replaceAll("#+\\s*", "")
                                  .replaceAll("\\*\\*", "")
                                  .replaceAll("\\*", "")
                                  .replaceAll("`", "")
                                  .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
                                  .replaceAll("!\\[[^\\]]*\\]\\([^\\)]+\\)", "") // 移除图片标记
                                  .trim();
        
        if (plainText.length() > 200) {
            return plainText.substring(0, 200) + "...";
        }
        return plainText;
    }
    
    /**
     * 从 Markdown 内容中提取图片
     */
    private List<ArticleImage> extractImages(String content, Long articleId) {
        List<ArticleImage> images = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "!\\[([^\\]]*)\\]\\(([^\\)]+)\\)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        int order = 0;
        while (matcher.find()) {
            ArticleImage image = new ArticleImage();
            image.setArticleId(articleId);
            image.setImageDesc(matcher.group(1));
            image.setImageUrl(matcher.group(2));
            image.setSortOrder(order++);
            images.add(image);
        }
        
        return images;
    }
}
