package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

/**
 * 文章DTO
 */
@Data
public class ArticleDTO {
    
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String summary;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private String coverImage;
    
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    
    private List<Long> tagIds;
    
    private Integer status;
}
