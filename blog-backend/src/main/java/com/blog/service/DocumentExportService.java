package com.blog.service;

import com.blog.entity.Article;
import com.blog.entity.KbDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文档导出服务
 */
@Slf4j
@Service
public class DocumentExportService {

    /**
     * 导出为Word文档
     */
    public byte[] exportToWord(KbDocument document) throws IOException {
        try (XWPFDocument wordDoc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 添加标题
            XWPFParagraph titlePara = wordDoc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(document.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();
            
            // 添加内容
            String content = document.getContent();
            if (content != null && !content.isEmpty()) {
                // 按段落分割
                String[] paragraphs = content.split("\n");
                for (String para : paragraphs) {
                    XWPFParagraph p = wordDoc.createParagraph();
                    XWPFRun run = p.createRun();
                    run.setText(para);
                }
            }
            
            wordDoc.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出为PDF文档
     */
    public byte[] exportToPdf(KbDocument document) throws IOException {
        try (PDDocument pdfDoc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            PDPage page = new PDPage();
            pdfDoc.addPage(page);
            
            // 加载中文字体
            PDFont font = loadChineseFont(pdfDoc);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page)) {
                contentStream.setFont(font, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                
                // 标题
                String title = document.getTitle();
                if (title != null) {
                    contentStream.showText(title);
                }
                
                contentStream.endText();
                
                // 内容
                contentStream.setFont(font, 12);
                String content = document.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] lines = content.split("\n");
                    float y = 710;
                    
                    for (String line : lines) {
                        if (y < 50) {
                            // 需要新页面
                            contentStream.close();
                            page = new PDPage();
                            pdfDoc.addPage(page);
                            try (PDPageContentStream newContent = new PDPageContentStream(pdfDoc, page)) {
                                newContent.setFont(font, 12);
                                newContent.beginText();
                                newContent.newLineAtOffset(50, 750);
                                y = 750;
                                
                                // 处理长行
                                String wrappedLine = wrapText(line, font, 12, 500);
                                newContent.showText(wrappedLine);
                                y -= 20;
                                newContent.endText();
                            }
                        } else {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(50, y);
                            // 处理长行
                            String wrappedLine = wrapText(line, font, 12, 500);
                            contentStream.showText(wrappedLine);
                            contentStream.endText();
                            y -= 20;
                        }
                    }
                }
            }
            
            pdfDoc.save(out);
            return out.toByteArray();
        }
    }

    /**
     * 导出为纯文本
     */
    public byte[] exportToTxt(KbDocument document) {
        StringBuilder sb = new StringBuilder();
        sb.append(document.getTitle()).append("\n");
        sb.append("=".repeat(50)).append("\n\n");
        
        if (document.getContent() != null) {
            sb.append(document.getContent());
        }
        
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 导出为 Markdown
     */
    public byte[] exportToMarkdown(KbDocument document) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(document.getTitle()).append("\n\n");
            
        if (document.getContent() != null) {
            sb.append(document.getContent());
        }
            
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
        
    /**
     * 导出文章为 Word 文档
     */
    public byte[] exportArticleToWord(Article article) throws IOException {
        try (XWPFDocument wordDoc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
            // 添加标题
            XWPFParagraph titlePara = wordDoc.createParagraph();
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(article.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.addBreak();
                
            // 添加摘要
            if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                XWPFParagraph summaryPara = wordDoc.createParagraph();
                XWPFRun summaryRun = summaryPara.createRun();
                summaryRun.setText("摘要：" + article.getSummary());
                summaryRun.setFontSize(12);
                summaryRun.addBreak();
            }
                
            // 添加内容
            String content = article.getContent();
            if (content != null && !content.isEmpty()) {
                String[] paragraphs = content.split("\n");
                for (String para : paragraphs) {
                    XWPFParagraph p = wordDoc.createParagraph();
                    XWPFRun run = p.createRun();
                    run.setText(para);
                }
            }
                
            wordDoc.write(out);
            return out.toByteArray();
        }
    }
    
    /**
     * 导出文章为 PDF 文档
     */
    public byte[] exportArticleToPdf(Article article) throws IOException {
        try (PDDocument pdfDoc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                
            PDPage page = new PDPage();
            pdfDoc.addPage(page);
                
            // 加载中文字体
            PDFont font = loadChineseFont(pdfDoc);
                
            try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page)) {
                contentStream.setFont(font, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                    
                // 标题
                String title = article.getTitle();
                if (title != null) {
                    contentStream.showText(title);
                }
                    
                contentStream.endText();
                    
                // 内容
                contentStream.setFont(font, 12);
                String content = article.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] lines = content.split("\n");
                    float y = 710;
                        
                    for (String line : lines) {
                        if (y < 50) {
                            // 需要新页面
                            contentStream.close();
                            page = new PDPage();
                            pdfDoc.addPage(page);
                            try (PDPageContentStream newContent = new PDPageContentStream(pdfDoc, page)) {
                                newContent.setFont(font, 12);
                                newContent.beginText();
                                newContent.newLineAtOffset(50, 750);
                                y = 750;
                                    
                                // 处理长行
                                String wrappedLine = wrapText(line, font, 12, 500);
                                newContent.showText(wrappedLine);
                                y -= 20;
                                newContent.endText();
                            }
                        } else {
                            contentStream.beginText();
                            contentStream.newLineAtOffset(50, y);
                            // 处理长行
                            String wrappedLine = wrapText(line, font, 12, 500);
                            contentStream.showText(wrappedLine);
                            contentStream.endText();
                            y -= 20;
                        }
                    }
                }
            }
                
            pdfDoc.save(out);
            return out.toByteArray();
        }
    }
    
    /**
     * 导出文章为纯文本
     */
    public byte[] exportArticleToTxt(Article article) {
        StringBuilder sb = new StringBuilder();
        sb.append(article.getTitle()).append("\n");
        sb.append("=".repeat(50)).append("\n\n");
            
        if (article.getSummary() != null) {
            sb.append("摘要：").append(article.getSummary()).append("\n\n");
        }
            
        if (article.getContent() != null) {
            sb.append(article.getContent());
        }
            
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    
    /**
     * 导出文章为 Markdown
     */
    public byte[] exportArticleToMarkdown(Article article) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(article.getTitle()).append("\n\n");
            
        if (article.getSummary() != null) {
            sb.append("**摘要：** ").append(article.getSummary()).append("\n\n");
        }
            
        if (article.getContent() != null) {
            sb.append(article.getContent());
        }
            
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 加载中文字体
     */
    private PDFont loadChineseFont(PDDocument document) throws IOException {
        // 尝试加载系统字体
        try (InputStream is = getClass().getResourceAsStream("/fonts/simsun.ttc")) {
            if (is != null) {
                return PDType0Font.load(document, is);
            }
        } catch (Exception e) {
            log.debug("未找到simsun字体，尝试其他方案");
        }
        
        // 使用内置字体作为后备
        try {
            // 尝试加载思源黑体或其他常见中文字体
            String[] fontPaths = {
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/msyh.ttc",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/PingFang.ttc"
            };
            
            for (String path : fontPaths) {
                java.io.File fontFile = new java.io.File(path);
                if (fontFile.exists()) {
                    return PDType0Font.load(document, fontFile);
                }
            }
        } catch (Exception e) {
            log.debug("未找到系统中文字体");
        }
        
        // 返回一个基本字体（可能不支持中文）
        log.warn("未找到中文字体，PDF导出可能无法正确显示中文");
        return PDType1Font.HELVETICA;
    }

    /**
     * 文本换行处理
     */
    private String wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            currentLine.append(c);
            try {
                float width = font.getStringWidth(currentLine.toString()) * fontSize / 1000f;
                if (width > maxWidth) {
                    // 移除最后一个字符并换行
                    currentLine.deleteCharAt(currentLine.length() - 1);
                    result.append(currentLine).append("\n");
                    currentLine = new StringBuilder().append(c);
                }
            } catch (Exception e) {
                // 某些字符可能无法计算宽度，跳过
            }
        }
        
        result.append(currentLine);
        return result.toString();
    }
}
