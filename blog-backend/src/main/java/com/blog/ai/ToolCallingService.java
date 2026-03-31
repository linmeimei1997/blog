package com.blog.ai;

import com.blog.entity.Article;
import com.blog.entity.KbDocument;
import com.blog.service.ArticleService;
import com.blog.service.KbDocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工具调用服务 - 增强版
 * 支持结构化参数解析、执行日志、重试机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallingService {

    private final ArticleService articleService;
    private final KbDocumentService kbDocumentService;
    private final WebParseService webParseService;
    private final ToolExecutionLogger executionLogger;
    private final ObjectMapper objectMapper;

    private final Map<String, ToolDefinition> tools = new HashMap<>();

    @PostConstruct
    private void initTools() {
        // 获取文章列表
        registerTool("get_article_list", "获取文章列表",
                Map.of("limit", "integer,可选,返回数量限制,默认10"),
                params -> {
                    try {
                        int limit = extractIntParam(params, "limit", 10);
                        List<Article> articles = articleService.getLatest(limit);
                        return ToolResult.success(articles);
                    } catch (Exception e) {
                        return ToolResult.error("获取文章列表失败: " + e.getMessage());
                    }
                });

        // 获取文章详情
        registerTool("get_article_detail", "获取文章详情",
                Map.of("id", "integer,必填,文章ID"),
                params -> {
                    try {
                        Long id = extractId(params);
                        Article article = articleService.getById(id);
                        if (article == null) {
                            return ToolResult.error("文章不存在");
                        }
                        Map<String, Object> result = new HashMap<>();
                        result.put("id", article.getId());
                        result.put("title", article.getTitle());
                        result.put("content", article.getContent());
                        result.put("summary", article.getSummary());
                        return ToolResult.success(result);
                    } catch (Exception e) {
                        return ToolResult.error("获取文章详情失败: " + e.getMessage());
                    }
                });

        // 搜索知识库
        registerTool("search_knowledge_base", "搜索知识库",
                Map.of("keyword", "string,必填,搜索关键词", "top_k", "integer,可选,返回数量,默认5"),
                params -> {
                    try {
                        String keyword = extractKeyword(params);
                        int topK = extractIntParam(params, "top_k", 5);
                        List<String> results = kbDocumentService.searchChunks(keyword);
                        return ToolResult.success(results.stream().limit(topK).collect(Collectors.toList()));
                    } catch (Exception e) {
                        return ToolResult.error("搜索知识库失败: " + e.getMessage());
                    }
                });

        // 总结文档
        registerTool("summarize_document", "总结文档内容",
                Map.of("id", "integer,必填,文档ID"),
                params -> {
                    try {
                        Long id = extractId(params);
                        String summary = kbDocumentService.parseContent(id);
                        return ToolResult.success(Map.of("summary", summary));
                    } catch (Exception e) {
                        return ToolResult.error("总结文档失败: " + e.getMessage());
                    }
                });

        // 创建博客文章
        registerTool("create_blog_article", "创建博客文章",
                Map.of("title", "string,必填,文章标题", "content", "string,必填,文章内容",
                        "summary", "string,可选,文章摘要"),
                params -> {
                    try {
                        return createArticleFromParams(params);
                    } catch (Exception e) {
                        log.error("创建博客文章失败", e);
                        return ToolResult.error("创建博客文章失败: " + e.getMessage());
                    }
                });

        // 获取知识库文档列表
        registerTool("list_kb_documents", "获取知识库文档列表",
                Map.of(),
                params -> {
                    try {
                        List<KbDocument> docs = kbDocumentService.list(null);
                        List<Map<String, Object>> result = docs.stream().map(d -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", d.getId());
                            map.put("title", d.getTitle());
                            map.put("fileType", d.getFileType());
                            map.put("uploadTime", d.getUploadTime());
                            return map;
                        }).collect(Collectors.toList());
                        return ToolResult.success(result);
                    } catch (Exception e) {
                        return ToolResult.error("获取文档列表失败: " + e.getMessage());
                    }
                });

        // 搜索文章
        registerTool("search_articles", "搜索文章",
                Map.of("keyword", "string,必填,搜索关键词", "limit", "integer,可选,返回数量,默认20"),
                params -> {
                    try {
                        String keyword = extractKeyword(params);
                        int limit = extractIntParam(params, "limit", 20);
                        List<Article> articles = articleService.getLatest(limit);
                        List<Map<String, Object>> result = articles.stream()
                                .filter(a -> keyword == null || keyword.isEmpty() ||
                                        a.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                                        (a.getContent() != null && a.getContent().toLowerCase().contains(keyword.toLowerCase())))
                                .map(a -> {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("id", a.getId());
                                    map.put("title", a.getTitle());
                                    map.put("summary", a.getSummary());
                                    map.put("createTime", a.getCreateTime());
                                    return map;
                                })
                                .collect(Collectors.toList());
                        return ToolResult.success(result);
                    } catch (Exception e) {
                        return ToolResult.error("搜索文章失败: " + e.getMessage());
                    }
                });

        // 更新文章
        registerTool("update_article", "更新文章",
                Map.of("id", "integer,必填,文章ID", "title", "string,可选,新标题",
                        "content", "string,可选,新内容", "summary", "string,可选,新摘要"),
                params -> {
                    try {
                        Long id = extractId(params);
                        String title = extractParam(params, "title");
                        String content = extractParam(params, "content");
                        String summary = extractParam(params, "summary");

                        Article article = articleService.getById(id);
                        if (article == null) {
                            return ToolResult.error("文章不存在，ID: " + id);
                        }

                        if (title != null && !title.isEmpty()) article.setTitle(title);
                        if (content != null && !content.isEmpty()) article.setContent(content);
                        if (summary != null && !summary.isEmpty()) article.setSummary(summary);

                        articleService.updateEntity(article);
                        log.info("AI更新文章成功: id={}", id);
                        return ToolResult.success("文章更新成功！ID: " + id + "，标题: " + article.getTitle());
                    } catch (Exception e) {
                        log.error("更新文章失败", e);
                        return ToolResult.error("更新文章失败: " + e.getMessage());
                    }
                });

        // 删除文章
        registerTool("delete_article", "删除文章",
                Map.of("id", "integer,可选,文章ID", "keyword", "string,可选,标题关键字"),
                params -> {
                    try {
                        Long id = extractId(params);

                        Pattern idPattern = Pattern.compile("\\d+");
                        Matcher idMatcher = idPattern.matcher(params);
                        boolean hasId = idMatcher.find();

                        if (hasId) {
                            Article article = articleService.getById(id);
                            if (article == null) {
                                return ToolResult.error("文章不存在，ID: " + id);
                            }
                            String title = article.getTitle();
                            articleService.delete(id);
                            log.info("AI删除文章成功: id={}, title={}", id, title);
                            return ToolResult.success("文章删除成功！ID: " + id + "，标题: " + title);
                        } else {
                            String keyword = extractKeyword(params);
                            if (keyword == null || keyword.isEmpty()) {
                                return ToolResult.error("请提供要删除的文章ID或标题关键字");
                            }

                            List<Article> articles = articleService.getLatest(50);
                            List<Article> matched = articles.stream()
                                    .filter(a -> a.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                                    .collect(Collectors.toList());

                            if (matched.isEmpty()) {
                                return ToolResult.error("没有找到标题包含 \"" + keyword + "\" 的文章");
                            } else if (matched.size() == 1) {
                                Article article = matched.get(0);
                                String title = article.getTitle();
                                articleService.delete(article.getId());
                                log.info("AI按关键字删除文章成功: id={}, title={}, keyword={}", article.getId(), title, keyword);
                                return ToolResult.success("文章删除成功！ID: " + article.getId() + "，标题: " + title);
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append("找到 ").append(matched.size()).append(" 篇包含 \"").append(keyword).append("\" 的文章：\n");
                                for (int i = 0; i < Math.min(matched.size(), 5); i++) {
                                    Article a = matched.get(i);
                                    sb.append((i + 1)).append(". ID: ").append(a.getId())
                                            .append("，标题: ").append(a.getTitle()).append("\n");
                                }
                                if (matched.size() > 5) {
                                    sb.append("... 还有 ").append(matched.size() - 5).append(" 篇\n");
                                }
                                sb.append("请提供具体的文章ID来确认删除哪一篇。");
                                return ToolResult.success(sb.toString());
                            }
                        }
                    } catch (Exception e) {
                        log.error("删除文章失败", e);
                        return ToolResult.error("删除文章失败: " + e.getMessage());
                    }
                });

        // 解析URL
        registerTool("parse_webpage", "解析URL结构",
                Map.of("url", "string,必填,要解析的URL"),
                params -> {
                    try {
                        String url = extractUrl(params);
                        if (url == null || url.isEmpty()) {
                            return ToolResult.error("请提供要分析的URL");
                        }

                        log.info("AI请求分析URL结构: {}", url);
                        Map<String, Object> parseResult = webParseService.parseUrl(url);

                        if (parseResult.containsKey("error")) {
                            return ToolResult.error("URL分析失败: " + parseResult.get("error"));
                        }

                        return ToolResult.success(parseResult);
                    } catch (Exception e) {
                        log.error("URL分析失败", e);
                        return ToolResult.error("URL分析失败: " + e.getMessage());
                    }
                });
    }

    /**
     * 注册工具
     */
    private void registerTool(String name, String description, Map<String, String> parameters,
                              Function<String, ToolResult> executor) {
        ToolDefinition def = new ToolDefinition();
        def.setName(name);
        def.setDescription(description);
        def.setParameters(parameters);
        def.setExecutor(executor);
        tools.put(name, def);
    }

    /**
     * 从AI响应中提取文章信息并创建
     */
    public ToolResult createArticle(String title, String content, String summary, Long userId) {
        try {
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary != null ? summary : content.substring(0, Math.min(200, content.length())));
            article.setAuthorId(userId);
            article.setCategoryId(1L);
            article.setStatus(1);
            article.setViewCount(0);
            article.setLikeCount(0);

            articleService.save(article);
            kbDocumentService.saveAsDocument(title, content, userId);

            log.info("AI创建文章成功: id={}, title={}", article.getId(), title);
            return ToolResult.success(Map.of(
                    "id", article.getId(),
                    "title", title,
                    "message", "文章创建成功！"
            ));
        } catch (Exception e) {
            log.error("创建文章失败", e);
            return ToolResult.error("创建文章失败: " + e.getMessage());
        }
    }

    public ToolResult createArticleFromParams(String params) {
        String title = extractParam(params, "title");
        String content = extractParam(params, "content");
        String summary = extractParam(params, "summary");

        if (title == null || content == null) {
            return ToolResult.error("创建文章失败：缺少标题或内容");
        }

        return createArticle(title, content, summary, 1L);
    }

    /**
     * 执行工具调用（带日志记录）
     */
    public ToolExecutionResult executeTools(String sessionId, String messageId, String message) {
        ToolExecutionResult result = new ToolExecutionResult();
        List<ToolCallRecord> records = new ArrayList<>();

        for (Map.Entry<String, ToolDefinition> entry : tools.entrySet()) {
            String toolName = entry.getKey();
            ToolDefinition tool = entry.getValue();

            if (shouldInvokeTool(message, toolName)) {
                log.info("调用工具: {}", toolName);

                ToolCallRecord record = new ToolCallRecord();
                record.setToolName(toolName);
                record.setInput(message);

                long startTime = System.currentTimeMillis();
                try {
                    ToolResult toolResult = tool.getExecutor().apply(message);
                    record.setSuccess(toolResult.isSuccess());
                    record.setOutput(toolResult.getData());
                    record.setError(toolResult.getError());
                    record.setDurationMs(System.currentTimeMillis() - startTime);

                    if (toolResult.isSuccess()) {
                        result.addResult(toolName, toolResult.getData());
                    } else {
                        result.addError(toolName, toolResult.getError());
                    }
                } catch (Exception e) {
                    record.setSuccess(false);
                    record.setError(e.getMessage());
                    record.setDurationMs(System.currentTimeMillis() - startTime);
                    result.addError(toolName, e.getMessage());
                    log.error("工具执行异常: {}", toolName, e);
                }

                records.add(record);
            }
        }

        result.setRecords(records);
        return result;
    }

    /**
     * 简化的工具执行（向后兼容）
     */
    public String executeTools(String message) {
        ToolExecutionResult result = executeTools(null, null, message);
        return result.formatForPrompt();
    }

    private boolean shouldInvokeTool(String message, String toolName) {
        String lowerMsg = message.toLowerCase();
        return switch (toolName) {
            case "get_article_list" ->
                    lowerMsg.contains("文章列表") || lowerMsg.contains("所有文章") || lowerMsg.contains("最新文章");
            case "get_article_detail" -> lowerMsg.contains("文章详情") || lowerMsg.contains("查看文章") ||
                    lowerMsg.contains("文章") && lowerMsg.matches(".*\\d+.*");
            case "search_knowledge_base" ->
                    lowerMsg.contains("搜索") || lowerMsg.contains("查找") || lowerMsg.contains("知识库");
            case "summarize_document" ->
                    lowerMsg.contains("总结") || lowerMsg.contains("摘要") || lowerMsg.contains("概括");
            case "create_blog_article" ->
                    lowerMsg.contains("写博客") || lowerMsg.contains("写文章") || lowerMsg.contains("生成文章") ||
                            lowerMsg.contains("创建博客") || lowerMsg.contains("发布文章") || lowerMsg.contains("写一篇");
            case "list_kb_documents" ->
                    lowerMsg.contains("文档列表") || lowerMsg.contains("知识库文档") || lowerMsg.contains("所有文档");
            case "search_articles" ->
                    lowerMsg.contains("搜索文章") || lowerMsg.contains("查找文章") || lowerMsg.contains("文章搜索") ||
                            lowerMsg.contains("找文章") || lowerMsg.contains("搜索笔记") || lowerMsg.contains("查找笔记");
            case "update_article" ->
                    lowerMsg.contains("修改文章") || lowerMsg.contains("更新文章") || lowerMsg.contains("编辑文章") ||
                            lowerMsg.contains("修改笔记") || lowerMsg.contains("更新笔记") || lowerMsg.contains("编辑笔记");
            case "delete_article" -> lowerMsg.contains("删除文章") || lowerMsg.contains("移除文章") ||
                    lowerMsg.contains("删除笔记") || lowerMsg.contains("移除笔记");
            case "parse_webpage" -> lowerMsg.contains("http://") || lowerMsg.contains("https://") ||
                    lowerMsg.contains("解析") && (lowerMsg.contains("网页") || lowerMsg.contains("网站")) ||
                    lowerMsg.contains("分析") && (lowerMsg.contains("网页") || lowerMsg.contains("网站"));
            default -> false;
        };
    }

    private Long extractId(String params) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(params);
        if (matcher.find()) {
            return Long.parseLong(matcher.group());
        }
        return 1L;
    }

    private int extractIntParam(String params, String key, int defaultValue) {
        Pattern pattern = Pattern.compile(key + "[=:]\\s*(\\d+)");
        Matcher matcher = pattern.matcher(params);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String extractKeyword(String params) {
        Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(params);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // 尝试提取"搜索"或"查找"后面的内容
        Pattern keywordPattern = Pattern.compile("(?:搜索|查找|找)\\s*[\"']?([^\"'\\s]+)[\"']?");
        Matcher keywordMatcher = keywordPattern.matcher(params);
        if (keywordMatcher.find()) {
            return keywordMatcher.group(1);
        }
        return params.length() > 20 ? params.substring(0, 20) : params;
    }

    private String extractUrl(String params) {
        Pattern urlPattern = Pattern.compile("https?://[^\\s<>\"{}|\\\\^`\\[\\]]+");
        Matcher matcher = urlPattern.matcher(params);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractParam(String params, String key) {
        Pattern pattern = Pattern.compile(key + "[=:]\\s*[\"']?([^\"'&\\s]+)[\"']?");
        Matcher matcher = pattern.matcher(params);
        if (matcher.find()) {
            try {
                return java.net.URLDecoder.decode(matcher.group(1), "UTF-8");
            } catch (Exception e) {
                return matcher.group(1);
            }
        }
        return null;
    }

    // 内部类
    @lombok.Data
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, String> parameters;
        private Function<String, ToolResult> executor;
    }

    @lombok.Data
    public static class ToolResult {
        private boolean success;
        private Object data;
        private String error;

        public static ToolResult success(Object data) {
            ToolResult result = new ToolResult();
            result.success = true;
            result.data = data;
            return result;
        }

        public static ToolResult error(String error) {
            ToolResult result = new ToolResult();
            result.success = false;
            result.error = error;
            return result;
        }
    }

    @lombok.Data
    public static class ToolExecutionResult {
        private List<ToolCallRecord> records = new ArrayList<>();
        private Map<String, Object> results = new HashMap<>();
        private Map<String, String> errors = new HashMap<>();

        public void addResult(String toolName, Object result) {
            results.put(toolName, result);
        }

        public void addError(String toolName, String error) {
            errors.put(toolName, error);
        }

        public String formatForPrompt() {
            StringBuilder sb = new StringBuilder();
            results.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            errors.forEach((k, v) -> sb.append(k).append(" [错误]: ").append(v).append("\n"));
            return sb.toString();
        }
    }

    @lombok.Data
    public static class ToolCallRecord {
        private String toolName;
        private String input;
        private Object output;
        private boolean success;
        private String error;
        private long durationMs;
    }
}