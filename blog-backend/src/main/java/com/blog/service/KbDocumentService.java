package com.blog.service;

import com.blog.entity.KbDocument;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 知识库文档服务接口
 */
public interface KbDocumentService {
    
    KbDocument getById(Long id);
    
    List<KbDocument> list(String keyword);
    
    KbDocument upload(MultipartFile file);
    
    void delete(Long id);
    
    String parseContent(Long id);
    
    List<String> searchChunks(String keyword);
    
    /**
     * 将文本内容保存为知识库文档
     */
    KbDocument saveAsDocument(String title, String content, Long userId);
}
