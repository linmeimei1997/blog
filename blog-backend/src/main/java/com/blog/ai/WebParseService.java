package com.blog.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * URL解析服务
 * 用于静态分析URL结构（不访问外部网站）
 */
@Slf4j
@Service
public class WebParseService {

    /**
     * 静态分析URL结构
     * 注意：此服务不会访问外部网站，仅基于URL字符串进行分析
     * @param url 网页URL
     * @return 解析结果，包含协议、域名、路径等结构信息
     */
    public Map<String, Object> parseUrl(String url) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始静态分析URL结构: {}", url);
            
            // 解析URL
            URL parsedUrl = new URL(url);
            
            // 基本信息
            result.put("url", url);
            result.put("protocol", parsedUrl.getProtocol());
            result.put("domain", parsedUrl.getHost());
            result.put("port", parsedUrl.getPort() > 0 ? parsedUrl.getPort() : getDefaultPort(parsedUrl.getProtocol()));
            result.put("path", parsedUrl.getPath());
            result.put("query", parsedUrl.getQuery());
            result.put("fragment", parsedUrl.getRef());
            
            // 分析域名
            String domain = parsedUrl.getHost();
            result.put("domainType", analyzeDomainType(domain));
            result.put("tld", extractTld(domain));
            
            // 分析路径
            String path = parsedUrl.getPath();
            result.put("pathSegments", path.split("/").length - 1);
            result.put("fileExtension", extractFileExtension(path));
            result.put("resourceType", analyzeResourceType(path));
            
            // 分析查询参数
            String query = parsedUrl.getQuery();
            if (query != null && !query.isEmpty()) {
                result.put("hasQueryParams", true);
                result.put("queryParams", parseQueryParams(query));
            } else {
                result.put("hasQueryParams", false);
            }
            
            // 网站类型推测
            result.put("siteType", guessSiteType(domain, path));
            
            // 内容类型推测
            result.put("contentType", guessContentType(domain, path));
            
            log.info("URL分析完成: domain={}, path={}", domain, path);
            
        } catch (Exception e) {
            log.error("URL解析失败: url={}, error={}", url, e.getMessage());
            result.put("error", "URL解析失败: " + e.getMessage());
            result.put("url", url);
        }
        
        return result;
    }
    
    /**
     * 获取默认端口
     */
    private int getDefaultPort(String protocol) {
        return switch (protocol.toLowerCase()) {
            case "http" -> 80;
            case "https" -> 443;
            case "ftp" -> 21;
            default -> -1;
        };
    }
    
    /**
     * 分析域名类型
     */
    private String analyzeDomainType(String domain) {
        if (domain.startsWith("www.")) {
            return "标准Web站点";
        } else if (domain.matches(".*\\.(gov|edu|org|com|net|cn|io|co)\\..*")) {
            return "多级域名";
        } else if (domain.contains("blog") || domain.contains("article")) {
            return "博客/文章类";
        } else if (domain.contains("shop") || domain.contains("store") || domain.contains("mall")) {
            return "电商类";
        } else if (domain.contains("news") || domain.contains("media")) {
            return "新闻媒体类";
        } else if (domain.contains("doc") || domain.contains("wiki") || domain.contains("docs")) {
            return "文档/知识库类";
        }
        return "普通网站";
    }
    
    /**
     * 提取顶级域名
     */
    private String extractTld(String domain) {
        String[] parts = domain.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        }
        return "";
    }
    
    /**
     * 提取文件扩展名
     */
    private String extractFileExtension(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        return "";
    }
    
    /**
     * 分析资源类型
     */
    private String analyzeResourceType(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "首页/目录页";
        }
        
        String extension = extractFileExtension(path);
        if (!extension.isEmpty()) {
            return switch (extension.toLowerCase()) {
                case "html", "htm" -> "HTML页面";
                case "pdf" -> "PDF文档";
                case "doc", "docx" -> "Word文档";
                case "xls", "xlsx" -> "Excel文档";
                case "ppt", "pptx" -> "PPT文档";
                case "json", "xml" -> "数据接口";
                case "jpg", "jpeg", "png", "gif", "webp" -> "图片资源";
                case "mp4", "avi", "mov" -> "视频资源";
                case "mp3", "wav" -> "音频资源";
                default -> "静态资源";
            };
        }
        
        // 根据路径关键词判断
        String lowerPath = path.toLowerCase();
        if (lowerPath.contains("/blog/") || lowerPath.contains("/article/") || lowerPath.contains("/post/")) {
            return "文章/博客页";
        } else if (lowerPath.contains("/product/") || lowerPath.contains("/item/") || lowerPath.contains("/goods/")) {
            return "商品详情页";
        } else if (lowerPath.contains("/category/") || lowerPath.contains("/tag/") || lowerPath.contains("/list/")) {
            return "分类/列表页";
        } else if (lowerPath.contains("/search") || lowerPath.contains("/query")) {
            return "搜索结果页";
        } else if (lowerPath.contains("/user/") || lowerPath.contains("/profile/") || lowerPath.contains("/author/")) {
            return "用户/作者页";
        } else if (lowerPath.contains("/api/")) {
            return "API接口";
        }
        
        return "动态页面";
    }
    
    /**
     * 解析查询参数
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                String key = pair.substring(0, eqIdx);
                String value = eqIdx < pair.length() - 1 ? pair.substring(eqIdx + 1) : "";
                params.put(key, value);
            }
        }
        return params;
    }
    
    /**
     * 猜测网站类型
     */
    private String guessSiteType(String domain, String path) {
        String lowerDomain = domain.toLowerCase();
        
        // 技术类网站
        if (lowerDomain.contains("github") || lowerDomain.contains("gitlab") || 
            lowerDomain.contains("stackoverflow") || lowerDomain.contains("csdn") ||
            lowerDomain.contains("juejin") || lowerDomain.contains("segmentfault")) {
            return "技术社区/代码托管";
        }
        
        // 新闻媒体
        if (lowerDomain.contains("news") || lowerDomain.contains("bbc") || 
            lowerDomain.contains("cnn") || lowerDomain.contains("sina") ||
            lowerDomain.contains("qq") || lowerDomain.contains("163")) {
            return "新闻媒体";
        }
        
        // 社交媒体
        if (lowerDomain.contains("twitter") || lowerDomain.contains("facebook") ||
            lowerDomain.contains("instagram") || lowerDomain.contains("weibo") ||
            lowerDomain.contains("zhihu") || lowerDomain.contains("douban")) {
            return "社交媒体";
        }
        
        // 电商平台
        if (lowerDomain.contains("taobao") || lowerDomain.contains("jd") ||
            lowerDomain.contains("amazon") || lowerDomain.contains("ebay") ||
            lowerDomain.contains("shop")) {
            return "电商平台";
        }
        
        // 视频网站
        if (lowerDomain.contains("youtube") || lowerDomain.contains("bilibili") ||
            lowerDomain.contains("youku") || lowerDomain.contains("iqiyi") ||
            lowerDomain.contains("video")) {
            return "视频网站";
        }
        
        // 博客类
        if (lowerDomain.contains("blog") || lowerDomain.contains("wordpress") ||
            lowerDomain.contains("medium") || lowerDomain.contains("substack")) {
            return "博客平台";
        }
        
        // 文档/知识库
        if (lowerDomain.contains("docs") || lowerDomain.contains("wiki") ||
            lowerDomain.contains("readme") || lowerDomain.contains("notion")) {
            return "文档/知识库";
        }
        
        return "普通网站";
    }
    
    /**
     * 猜测内容类型
     */
    private String guessContentType(String domain, String path) {
        String lowerDomain = domain.toLowerCase();
        String lowerPath = path.toLowerCase();
        
        // 技术文档
        if (lowerDomain.contains("docs") || lowerPath.contains("/docs/") ||
            lowerPath.contains("/documentation/") || lowerPath.contains("/api/")) {
            return "技术文档";
        }
        
        // 教程/指南
        if (lowerPath.contains("/tutorial/") || lowerPath.contains("/guide/") ||
            lowerPath.contains("/learn/") || lowerPath.contains("/course/")) {
            return "教程/指南";
        }
        
        // 新闻资讯
        if (lowerPath.contains("/news/") || lowerPath.contains("/article/") ||
            lowerPath.contains("/blog/") || lowerPath.contains("/post/")) {
            return "文章/资讯";
        }
        
        // 产品介绍
        if (lowerPath.contains("/product/") || lowerPath.contains("/feature/") ||
            lowerPath.contains("/about")) {
            return "产品介绍";
        }
        
        return "通用网页";
    }
}
