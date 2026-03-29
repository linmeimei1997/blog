package com.blog.service.impl;

import com.blog.entity.KbChunk;
import com.blog.entity.KbDocument;
import com.blog.mapper.KbChunkMapper;
import com.blog.mapper.KbDocumentMapper;
import com.blog.service.KbDocumentService;
import com.blog.util.DocumentParser;
import com.blog.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识库文档服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl implements KbDocumentService {

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final DocumentParser documentParser;

    @Value("${app.file.upload-path}")
    private String uploadPath;

    @Value("${app.file.allowed-types}")
    private String allowedTypes;

    @Override
    public KbDocument getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public List<KbDocument> list(String keyword) {
        return documentMapper.selectList(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument upload(MultipartFile file) {
        // 验证文件类型
        String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
        List<String> allowed = Arrays.asList(allowedTypes.split(","));
        if (!allowed.contains(extension)) {
            throw new RuntimeException("不支持的文件类型: " + extension);
        }

        // 创建上传目录
        Path uploadDir = Paths.get(uploadPath, "kb");
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败", e);
        }

        // 生成文件名
        String fileName = UUID.randomUUID().toString() + "." + extension;
        Path filePath = uploadDir.resolve(fileName);

        // 保存文件
        try {
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("保存文件失败", e);
        }

        // 解析文档内容
        String content = documentParser.parse(filePath.toFile(), extension);

        // 保存文档记录
        KbDocument document = new KbDocument();
        document.setTitle(FilenameUtils.getBaseName(file.getOriginalFilename()));
        document.setFileName(file.getOriginalFilename());
        document.setFileType(extension);
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setContent(content);
        document.setStatus(1);
        document.setUploadBy(SecurityUtils.getCurrentUserId());
        documentMapper.insert(document);

        // 分块处理
        List<String> chunks = splitContent(content, 500, 50);
        List<KbChunk> kbChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk chunk = new KbChunk();
            chunk.setDocumentId(document.getId());
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setStartPos(i * 450);
            chunk.setEndPos(i * 450 + chunks.get(i).length());
            kbChunks.add(chunk);
        }
        
        if (!kbChunks.isEmpty()) {
            chunkMapper.insertBatch(kbChunks);
            documentMapper.updateChunkCount(document.getId(), kbChunks.size());
        }

        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document != null) {
            // 删除文件
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
                log.warn("删除文件失败: {}", document.getFilePath());
            }
            
            // 删除分块
            chunkMapper.deleteByDocumentId(id);
            
            // 删除记录
            documentMapper.deleteById(id);
        }
    }

    @Override
    public String parseContent(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            return null;
        }
        return document.getContent();
    }

    @Override
    public List<String> searchChunks(String keyword) {
        List<KbChunk> chunks = chunkMapper.searchByKeyword(keyword);
        return chunks.stream()
                .map(KbChunk::getContent)
                .limit(5)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument saveAsDocument(String title, String content, Long userId) {
        // 保存文档记录（文本类型，无文件）
        KbDocument document = new KbDocument();
        document.setTitle(title);
        document.setFileName(title + ".txt");
        document.setFileType("txt");
        document.setFileSize((long) content.getBytes().length);
        document.setFilePath(""); // 文本内容无文件路径
        document.setContent(content);
        document.setStatus(1);
        document.setUploadBy(userId);
        documentMapper.insert(document);
        
        // 分块处理
        List<String> chunks = splitContent(content, 500, 50);
        List<KbChunk> kbChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk chunk = new KbChunk();
            chunk.setDocumentId(document.getId());
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setStartPos(i * 450);
            chunk.setEndPos(i * 450 + chunks.get(i).length());
            kbChunks.add(chunk);
        }
        
        if (!kbChunks.isEmpty()) {
            chunkMapper.insertBatch(kbChunks);
            documentMapper.updateChunkCount(document.getId(), kbChunks.size());
        }
        
        log.info("保存知识库文档成功: id={}, title={}", document.getId(), title);
        return document;
    }

    private List<String> splitContent(String content, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int step = chunkSize - overlap;
        for (int i = 0; i < content.length(); i += step) {
            int end = Math.min(i + chunkSize, content.length());
            chunks.add(content.substring(i, end));
            if (end == content.length()) {
                break;
            }
        }
        return chunks;
    }
}
