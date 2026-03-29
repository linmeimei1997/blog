package com.blog.mapper;

import com.blog.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文章标签关联Mapper
 */
@Mapper
public interface ArticleTagMapper {
    
    int insert(ArticleTag articleTag);
    
    int insertBatch(@Param("list") List<ArticleTag> list);
    
    int deleteByArticleId(@Param("articleId") Long articleId);
    
    int deleteByTagId(@Param("tagId") Long tagId);
    
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);
}
