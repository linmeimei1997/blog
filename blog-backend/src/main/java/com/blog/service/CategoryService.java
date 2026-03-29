package com.blog.service;

import com.blog.entity.Category;
import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {
    
    Category getById(Long id);
    
    List<Category> listAll();
    
    Long create(Category category);
    
    void update(Category category);
    
    void delete(Long id);
}
