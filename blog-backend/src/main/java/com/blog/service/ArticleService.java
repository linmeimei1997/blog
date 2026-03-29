package com.blog.service;

import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.dto.PageResult;
import com.blog.entity.Article;
import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService {
    
    Article getById(Long id);
    
    PageResult<Article> list(ArticleQuery query);
    
    Long create(ArticleDTO dto);
    
    void update(ArticleDTO dto);
    
    void delete(Long id);
    
    void incrementViewCount(Long id);
    
    List<Article> getLatest(Integer limit);
    
    List<Article> getByCategory(Long categoryId);
    
    /**
     * 直接保存文章实体（用于AI生成）
     */
    void save(Article article);
    
    /**
     * 直接更新文章实体（用于AI管理）
     */
    void updateEntity(Article article);
}
