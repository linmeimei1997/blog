package com.blog.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 文档解析工具
 */
@Slf4j
@Component
public class DocumentParser {

    /**
     * 解析文档
     */
    public String parse(File file, String extension) {
        try {
            return switch (extension.toLowerCase()) {
                case "txt", "md" -> parseText(file);
                case "pdf" -> parsePdf(file);
                case "doc" -> parseDoc(file);
                case "docx" -> parseDocx(file);
                default -> throw new RuntimeException("不支持的文件类型: " + extension);
            };
        } catch (Exception e) {
            log.error("解析文档失败: {}", file.getName(), e);
            throw new RuntimeException("解析文档失败: " + e.getMessage());
        }
    }

    /**
     * 解析文本文件
     */
    private String parseText(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * 解析 PDF
     */
    private String parsePdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 DOC
     */
    private String parseDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 解析 DOCX
     */
    private String parseDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }
}
