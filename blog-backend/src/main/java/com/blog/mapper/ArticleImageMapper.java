package com.blog.mapper;

import com.blog.entity.ArticleImage;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 文章图片 Mapper
 */
@Mapper
public interface ArticleImageMapper {
    
    @Insert("INSERT INTO article_image (article_id, image_url, image_desc, sort_order, create_time) " +
            "VALUES (#{articleId}, #{imageUrl}, #{imageDesc}, #{sortOrder}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ArticleImage image);
    
    @Select("SELECT * FROM article_image WHERE article_id = #{articleId} ORDER BY sort_order")
    List<ArticleImage> selectByArticleId(Long articleId);
    
    @Delete("DELETE FROM article_image WHERE article_id = #{articleId}")
    void deleteByArticleId(Long articleId);
    
    @Insert("<script>" +
            "INSERT INTO article_image (article_id, image_url, image_desc, sort_order, create_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.articleId}, #{item.imageUrl}, #{item.imageDesc}, #{item.sortOrder}, NOW())" +
            "</foreach>" +
            "</script>")
    void insertBatch(@Param("list") List<ArticleImage> images);
}
