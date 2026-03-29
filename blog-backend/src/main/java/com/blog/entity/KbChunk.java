package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库文档分块实体
 */
@Data
public class KbChunk {
    
    private Long id;
    
    private Long documentId;
    
    private String content;
    
    private String vectorId;
    
    private Integer chunkIndex;
    
    private Integer startPos;
    
    private Integer endPos;
    
    private LocalDateTime createTime;
}
