package com.blog.controller;

import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.dto.PageResult;
import com.blog.dto.Result;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import com.blog.service.DocumentExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文章控制器
 */
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final DocumentExportService documentExportService;

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            return Result.error("文章不存在");
        }
        return Result.success(article);
    }

    @GetMapping("/list")
    public Result<PageResult<Article>> list(ArticleQuery query) {
        PageResult<Article> result = articleService.list(query);
        return Result.success(result);
    }

    @PostMapping
    public Result<Long> create(@RequestBody ArticleDTO dto) {
        Long id = articleService.create(dto);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ArticleDTO dto) {
        dto.setId(id);
        articleService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/view")
    public Result<Void> incrementView(@PathVariable Long id) {
        articleService.incrementViewCount(id);
        return Result.success();
    }

    @GetMapping("/latest")
    public Result<List<Article>> getLatest(@RequestParam(defaultValue = "5") Integer limit) {
        List<Article> list = articleService.getLatest(limit);
        return Result.success(list);
    }
    
    /**
     * 导出文章为指定格式
     * @param id 文章 ID
     * @param format 导出格式：word, pdf, txt, md
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportArticle(
            @PathVariable Long id,
            @RequestParam(defaultValue = "word") String format) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] content;
            String filename;
            String contentType;
            
            switch (format.toLowerCase()) {
                case "word":
                case "docx":
                    content = documentExportService.exportArticleToWord(article);
                    filename = article.getTitle() + ".docx";
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    break;
                case "pdf":
                    content = documentExportService.exportArticleToPdf(article);
                    filename = article.getTitle() + ".pdf";
                    contentType = "application/pdf";
                    break;
                case "txt":
                    content = documentExportService.exportArticleToTxt(article);
                    filename = article.getTitle() + ".txt";
                    contentType = "text/plain";
                    break;
                case "md":
                case "markdown":
                    content = documentExportService.exportArticleToMarkdown(article);
                    filename = article.getTitle() + ".md";
                    contentType = "text/markdown";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }
            
            // 处理文件名编码
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content);
                    
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
