package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 标签实体
 */
@Data
public class Tag {
    
    private Long id;
    
    private String name;
    
    private Integer articleCount;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
