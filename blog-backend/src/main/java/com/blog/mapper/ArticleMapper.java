package com.blog.mapper;

import com.blog.dto.ArticleQuery;
import com.blog.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文章Mapper
 */
@Mapper
public interface ArticleMapper {
    
    Article selectById(@Param("id") Long id);
    
    List<Article> selectList(ArticleQuery query);
    
    Long countList(ArticleQuery query);
    
    int insert(Article article);
    
    int update(Article article);
    
    int deleteById(@Param("id") Long id);
    
    int incrementViewCount(@Param("id") Long id);
    
    List<Article> selectByCategory(@Param("categoryId") Long categoryId);
    
    List<Article> selectLatest(@Param("limit") Integer limit);
}
