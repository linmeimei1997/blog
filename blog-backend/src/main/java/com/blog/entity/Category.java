package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分类实体
 */
@Data
public class Category {
    
    private Long id;
    
    private String name;
    
    private String description;
    
    private Integer sortOrder;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
