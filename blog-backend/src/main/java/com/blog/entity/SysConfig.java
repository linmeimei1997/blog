package com.blog.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 */
@Data
public class SysConfig {
    
    private Long id;
    
    private String configKey;
    
    private String configValue;
    
    private String description;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
