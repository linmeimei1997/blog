package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文章标签关联实体
 */
@Data
public class ArticleTag {
    
    private Long id;
    
    private Long articleId;
    
    private Long tagId;
    
    private LocalDateTime createTime;
}
