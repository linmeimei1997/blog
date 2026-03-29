package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章实体
 */
@Data
public class Article {
    
    private Long id;
    
    private String title;
    
    private String summary;
    
    private String content;
    
    private String coverImage;
    
    private Long categoryId;
    
    private Long authorId;
    
    private Integer status;
    
    private Integer viewCount;
    
    private Integer likeCount;
    
    private LocalDateTime publishTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
    
    // 非数据库字段
    private String categoryName;
    private List<Tag> tags;
    private String authorName;
}
