package com.blog.ai;

import com.blog.entity.KbChunk;
import com.blog.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 检索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KbChunkMapper chunkMapper;

    /**
     * 检索相关上下文
     */
    public String retrieveContext(String query) {
        // 基于关键词检索
        List<KbChunk> chunks = chunkMapper.searchByKeyword(query);
        
        if (chunks.isEmpty()) {
            return null;
        }
        
        // 取前5个最相关的结果
        return chunks.stream()
                .limit(5)
                .map(KbChunk::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 检索文档
     */
    public List<String> searchDocuments(String query) {
        List<KbChunk> chunks = chunkMapper.searchByKeyword(query);
        return chunks.stream()
                .map(KbChunk::getContent)
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 总结文档
     */
    public String summarizeDocument(Long documentId) {
        List<KbChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return "文档为空或不存在";
        }
        
        String content = chunks.stream()
                .map(KbChunk::getContent)
                .collect(Collectors.joining("\n"));
        
        // 截取前2000字符作为摘要
        if (content.length() > 2000) {
            return content.substring(0, 2000) + "...";
        }
        return content;
    }
}
