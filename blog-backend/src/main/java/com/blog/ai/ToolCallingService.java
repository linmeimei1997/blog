package com.blog.ai;

import com.blog.entity.Article;
import com.blog.entity.KbDocument;
import com.blog.service.ArticleService;
import com.blog.service.KbDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 工具调用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolCallingService {

    private final ArticleService articleService;
    private final KbDocumentService kbDocumentService;
    private final WebParseService webParseService;
    private final ObjectMapper objectMapper;

    private final Map<String, Function<String, String>> tools = new HashMap<>();

    private void initTools() {
        // 获取文章列表
        tools.put("get_article_list", params -> {
            try {
                List<Article> articles = articleService.getLatest(10);
                return objectMapper.writeValueAsString(articles);
            } catch (Exception e) {
                return "获取文章列表失败: " + e.getMessage();
            }
        });

        // 获取文章详情
        tools.put("get_article_detail", params -> {
            try {
                Long id = extractId(params);
                Article article = articleService.getById(id);
                if (article == null) {
                    return "文章不存在";
                }
                Map<String, Object> result = new HashMap<>();
                result.put("id", article.getId());
                result.put("title", article.getTitle());
                result.put("content", article.getContent());
                result.put("summary", article.getSummary());
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                return "获取文章详情失败: " + e.getMessage();
            }
        });

        // 搜索知识库
        tools.put("search_knowledge_base", params -> {
            try {
                String keyword = extractKeyword(params);
                List<String> results = kbDocumentService.searchChunks(keyword);
                return objectMapper.writeValueAsString(results);
            } catch (Exception e) {
                return "搜索知识库失败: " + e.getMessage();
            }
        });

        // 总结文档
        tools.put("summarize_document", params -> {
            try {
                Long id = extractId(params);
                return kbDocumentService.parseContent(id);
            } catch (Exception e) {
                return "总结文档失败: " + e.getMessage();
            }
        });
        
        // 创建博客文章并保存到知识库
        tools.put("create_blog_article", params -> {
            try {
                return createArticleFromParams(params);
            } catch (Exception e) {
                log.error("创建博客文章失败", e);
                return "创建博客文章失败: " + e.getMessage();
            }
        });
        
        // 获取知识库文档列表
        tools.put("list_kb_documents", params -> {
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
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                return "获取文档列表失败: " + e.getMessage();
            }
        });
        
        // 搜索文章
        tools.put("search_articles", params -> {
            try {
                String keyword = extractKeyword(params);
                List<Article> articles = articleService.getLatest(20);
                // 简单关键词过滤
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
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                return "搜索文章失败: " + e.getMessage();
            }
        });
        
        // 更新文章
        tools.put("update_article", params -> {
            try {
                Long id = extractId(params);
                String title = extractParam(params, "title");
                String content = extractParam(params, "content");
                String summary = extractParam(params, "summary");
                
                Article article = articleService.getById(id);
                if (article == null) {
                    return "文章不存在，ID: " + id;
                }
                
                if (title != null && !title.isEmpty()) {
                    article.setTitle(title);
                }
                if (content != null && !content.isEmpty()) {
                    article.setContent(content);
                }
                if (summary != null && !summary.isEmpty()) {
                    article.setSummary(summary);
                }
                
                articleService.updateEntity(article);
                
                log.info("AI更新文章成功: id={}", id);
                return "文章更新成功！ID: " + id + "，标题: " + article.getTitle();
            } catch (Exception e) {
                log.error("更新文章失败", e);
                return "更新文章失败: " + e.getMessage();
            }
        });
        
        // 删除文章（支持按ID或关键字删除）
        tools.put("delete_article", params -> {
            try {
                // 先尝试提取ID
                Long id = extractId(params);
                
                // 检查params中是否真的有数字ID
                Pattern idPattern = Pattern.compile("\\d+");
                Matcher idMatcher = idPattern.matcher(params);
                boolean hasId = idMatcher.find();
                
                if (hasId) {
                    // 按ID删除
                    Article article = articleService.getById(id);
                    if (article == null) {
                        return "文章不存在，ID: " + id;
                    }
                    
                    String title = article.getTitle();
                    articleService.delete(id);
                    
                    log.info("AI删除文章成功: id={}, title={}", id, title);
                    return "文章删除成功！ID: " + id + "，标题: " + title;
                } else {
                    // 按关键字搜索并删除
                    String keyword = extractKeyword(params);
                    if (keyword == null || keyword.isEmpty()) {
                        return "请提供要删除的文章ID或标题关键字";
                    }
                    
                    // 搜索匹配的文章
                    List<Article> articles = articleService.getLatest(50);
                    List<Article> matched = articles.stream()
                        .filter(a -> a.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                        .collect(Collectors.toList());
                    
                    if (matched.isEmpty()) {
                        return "没有找到标题包含 \"" + keyword + "\" 的文章";
                    } else if (matched.size() == 1) {
                        // 只有一篇匹配，直接删除
                        Article article = matched.get(0);
                        String title = article.getTitle();
                        articleService.delete(article.getId());
                        log.info("AI按关键字删除文章成功: id={}, title={}, keyword={}", article.getId(), title, keyword);
                        return "文章删除成功！ID: " + article.getId() + "，标题: " + title;
                    } else {
                        // 多篇匹配，让用户确认
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
                        return sb.toString();
                    }
                }
            } catch (Exception e) {
                log.error("删除文章失败", e);
                return "删除文章失败: " + e.getMessage();
            }
        });
        
        // 解析URL结构（静态分析，不访问外部网站）
        tools.put("parse_webpage", params -> {
            try {
                String url = extractUrl(params);
                if (url == null || url.isEmpty()) {
                    return "请提供要分析的URL";
                }
                
                log.info("AI请求分析URL结构: {}", url);
                Map<String, Object> parseResult = webParseService.parseUrl(url);
                
                if (parseResult.containsKey("error")) {
                    return "URL分析失败: " + parseResult.get("error");
                }
                
                // 构建返回结果
                StringBuilder result = new StringBuilder();
                result.append("【URL结构分析结果】\n");
                result.append("完整URL: ").append(url).append("\n");
                result.append("协议: ").append(parseResult.get("protocol")).append("\n");
                result.append("域名: ").append(parseResult.get("domain")).append("\n");
                result.append("端口: ").append(parseResult.get("port")).append("\n");
                result.append("路径: ").append(parseResult.get("path")).append("\n");
                result.append("域名类型: ").append(parseResult.get("domainType")).append("\n");
                result.append("网站类型: ").append(parseResult.get("siteType")).append("\n");
                result.append("资源类型: ").append(parseResult.get("resourceType")).append("\n");
                result.append("内容类型: ").append(parseResult.get("contentType")).append("\n");
                
                String extension = (String) parseResult.get("fileExtension");
                if (extension != null && !extension.isEmpty()) {
                    result.append("文件扩展名: ").append(extension).append("\n");
                }
                
                Boolean hasQuery = (Boolean) parseResult.get("hasQueryParams");
                if (hasQuery != null && hasQuery) {
                    result.append("查询参数: ").append(parseResult.get("queryParams")).append("\n");
                }
                
                result.append("\n=== 分析说明 ===\n");
                result.append("这是基于URL结构的静态分析结果。\n");
                result.append("如需了解网页具体内容，请让用户复制粘贴网页内容给你。\n");
                result.append("你可以根据URL结构推断网站和内容类型，帮助用户理解链接指向的资源。");
                
                return result.toString();
            } catch (Exception e) {
                log.error("URL分析失败", e);
                return "URL分析失败: " + e.getMessage();
            }
        });
    }
    
    /**
     * 从AI响应中提取文章信息并创建
     */
    public String createArticle(String title, String content, String summary, Long userId) {
        try {
            // 创建文章
            Article article = new Article();
            article.setTitle(title);
            article.setContent(content);
            article.setSummary(summary != null ? summary : content.substring(0, Math.min(200, content.length())));
            article.setAuthorId(userId);
            article.setCategoryId(1L); // 默认分类
            article.setStatus(1); // 已发布
            article.setViewCount(0);
            article.setLikeCount(0);
            
            articleService.save(article);
            
            // 同时保存到知识库
            kbDocumentService.saveAsDocument(title, content, userId);
            
            log.info("AI创建文章成功: id={}, title={}", article.getId(), title);
            return "文章创建成功！标题：《" + title + "》，ID：" + article.getId();
        } catch (Exception e) {
            log.error("创建文章失败", e);
            return "创建文章失败: " + e.getMessage();
        }
    }
    
    public String createArticleFromParams(String params) {
        // 解析参数，格式: title=xxx&content=xxx&summary=xxx
        String title = extractParam(params, "title");
        String content = extractParam(params, "content");
        String summary = extractParam(params, "summary");
        
        if (title == null || content == null) {
            return "创建文章失败：缺少标题或内容";
        }
        
        // 默认使用系统用户
        return createArticle(title, content, summary, 1L);
    }
    
    private String extractParam(String params, String key) {
        Pattern pattern = Pattern.compile(key + "=([^&]+)");
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

    /**
     * 执行工具调用
     */
    public String executeTools(String message) {
        StringBuilder result = new StringBuilder();
        
        // 检测工具调用意图
        for (Map.Entry<String, Function<String, String>> entry : tools.entrySet()) {
            String toolName = entry.getKey();
            if (shouldInvokeTool(message, toolName)) {
                log.info("调用工具: {}", toolName);
                String toolResult = entry.getValue().apply(message);
                result.append(toolName).append(": ").append(toolResult).append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 判断是否应该调用工具
     */
    private boolean shouldInvokeTool(String message, String toolName) {
        String lowerMsg = message.toLowerCase();
        return switch (toolName) {
            case "get_article_list" -> 
                lowerMsg.contains("文章列表") || lowerMsg.contains("所有文章") || lowerMsg.contains("最新文章");
            case "get_article_detail" -> 
                lowerMsg.contains("文章详情") || lowerMsg.contains("查看文章") || 
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
                lowerMsg.contains("找文章") || lowerMsg.contains("搜索笔记") || lowerMsg.contains("查找笔记") ||
                (lowerMsg.contains("文章") && lowerMsg.contains("关键词")) ||
                (lowerMsg.contains("笔记") && lowerMsg.contains("关键词"));
            case "update_article" -> 
                lowerMsg.contains("修改文章") || lowerMsg.contains("更新文章") || lowerMsg.contains("编辑文章") ||
                lowerMsg.contains("更改文章") || lowerMsg.contains("修改笔记") || lowerMsg.contains("更新笔记") ||
                lowerMsg.contains("编辑笔记") || lowerMsg.contains("更改笔记") ||
                (lowerMsg.contains("文章") && lowerMsg.contains("修改")) ||
                (lowerMsg.contains("文章") && lowerMsg.contains("编辑")) ||
                (lowerMsg.contains("笔记") && lowerMsg.contains("修改")) ||
                (lowerMsg.contains("笔记") && lowerMsg.contains("编辑"));
            case "delete_article" -> 
                lowerMsg.contains("删除文章") || lowerMsg.contains("移除文章") || 
                lowerMsg.contains("删除笔记") || lowerMsg.contains("移除笔记") ||
                (lowerMsg.contains("文章") && lowerMsg.contains("删除")) ||
                (lowerMsg.contains("笔记") && lowerMsg.contains("删除"));
            case "parse_webpage" ->
                // 包含URL就尝试解析
                lowerMsg.contains("http://") || lowerMsg.contains("https://") ||
                lowerMsg.contains("解析") && (lowerMsg.contains("网页") || lowerMsg.contains("网站") || lowerMsg.contains("链接")) ||
                lowerMsg.contains("分析") && (lowerMsg.contains("网页") || lowerMsg.contains("网站") || lowerMsg.contains("链接")) ||
                lowerMsg.contains("解析url") || lowerMsg.contains("解析网址") ||
                lowerMsg.contains("抓取") && (lowerMsg.contains("网页") || lowerMsg.contains("网站")) ||
                lowerMsg.contains("读取") && (lowerMsg.contains("网页") || lowerMsg.contains("网站"));
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

    private String extractKeyword(String params) {
        // 简单提取引号中的内容或前10个字符
        Pattern pattern = Pattern.compile("[\"']([^\"']+)[\"']");
        Matcher matcher = pattern.matcher(params);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return params.length() > 10 ? params.substring(0, 10) : params;
    }
    
    /**
     * 从消息中提取URL
     */
    private String extractUrl(String params) {
        // 匹配 http:// 或 https:// 开头的URL
        Pattern urlPattern = Pattern.compile("https?://[^\\s<>\"{}|\\\\^`\\[\\]]+");
        Matcher matcher = urlPattern.matcher(params);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
