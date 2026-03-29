package com.blog.dto;

import lombok.Data;

/**
 * 文章查询条件
 */
@Data
public class ArticleQuery {
    
    private String keyword;
    private Long categoryId;
    private Long tagId;
    private Integer status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
