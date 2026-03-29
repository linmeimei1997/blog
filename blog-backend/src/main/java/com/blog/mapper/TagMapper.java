package com.blog.mapper;

import com.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 标签Mapper
 */
@Mapper
public interface TagMapper {
    
    Tag selectById(@Param("id") Long id);
    
    Tag selectByName(@Param("name") String name);
    
    List<Tag> selectAll();
    
    List<Tag> selectByArticleId(@Param("articleId") Long articleId);
    
    int insert(Tag tag);
    
    int update(Tag tag);
    
    int deleteById(@Param("id") Long id);
    
    int incrementArticleCount(@Param("id") Long id);
}
