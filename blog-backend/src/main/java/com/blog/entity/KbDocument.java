package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
public class KbDocument {
    
    private Long id;
    
    private String title;
    
    private String fileName;
    
    private String fileType;
    
    private Long fileSize;
    
    private String filePath;
    
    private String content;
    
    private Integer status;
    
    private Integer chunkCount;
    
    private Long uploadBy;
    
    private LocalDateTime uploadTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
