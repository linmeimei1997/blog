package com.blog.mapper;

import com.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 分类Mapper
 */
@Mapper
public interface CategoryMapper {
    
    Category selectById(@Param("id") Long id);
    
    List<Category> selectAll();
    
    int insert(Category category);
    
    int update(Category category);
    
    int deleteById(@Param("id") Long id);
}
