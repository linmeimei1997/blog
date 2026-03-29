package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文章图片实体（用于图文混排）
 */
@Data
public class ArticleImage {
    
    private Long id;
    
    private Long articleId;
    
    private String imageUrl;
    
    private String imageDesc;
    
    private Integer sortOrder;
    
    private LocalDateTime createTime;
}
