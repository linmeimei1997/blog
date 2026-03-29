package com.blog.controller;

import com.blog.dto.Result;
import com.blog.entity.KbDocument;
import com.blog.service.KbDocumentService;
import com.blog.service.DocumentExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识库文档控制器
 */
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService kbDocumentService;
    private final DocumentExportService documentExportService;

    @GetMapping("/list")
    public Result<List<KbDocument>> list(@RequestParam(required = false) String keyword) {
        List<KbDocument> list = kbDocumentService.list(keyword);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<KbDocument> getById(@PathVariable Long id) {
        KbDocument document = kbDocumentService.getById(id);
        if (document == null) {
            return Result.error("文档不存在");
        }
        return Result.success(document);
    }

    @PostMapping("/upload")
    public Result<KbDocument> upload(@RequestParam("file") MultipartFile file) {
        KbDocument document = kbDocumentService.upload(file);
        return Result.success(document);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        kbDocumentService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}/content")
    public Result<String> getContent(@PathVariable Long id) {
        String content = kbDocumentService.parseContent(id);
        return Result.success(content);
    }

    @GetMapping("/search")
    public Result<List<String>> search(@RequestParam String keyword) {
        List<String> results = kbDocumentService.searchChunks(keyword);
        return Result.success(results);
    }
    
    /**
     * 导出文档为指定格式
     * @param id 文档ID
     * @param format 导出格式：word, pdf, txt, md
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "word") String format) {
        try {
            KbDocument document = kbDocumentService.getById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] content;
            String filename;
            String contentType;
            String extension;
            
            switch (format.toLowerCase()) {
                case "word":
                case "docx":
                    content = documentExportService.exportToWord(document);
                    filename = document.getTitle() + ".docx";
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    extension = "docx";
                    break;
                case "pdf":
                    content = documentExportService.exportToPdf(document);
                    filename = document.getTitle() + ".pdf";
                    contentType = "application/pdf";
                    extension = "pdf";
                    break;
                case "txt":
                    content = documentExportService.exportToTxt(document);
                    filename = document.getTitle() + ".txt";
                    contentType = "text/plain";
                    extension = "txt";
                    break;
                case "md":
                case "markdown":
                    content = documentExportService.exportToMarkdown(document);
                    filename = document.getTitle() + ".md";
                    contentType = "text/markdown";
                    extension = "md";
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
