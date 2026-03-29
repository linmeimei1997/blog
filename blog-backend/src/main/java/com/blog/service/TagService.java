package com.blog.service;

import com.blog.entity.Tag;
import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {
    
    Tag getById(Long id);
    
    List<Tag> listAll();
    
    List<Tag> getByArticleId(Long articleId);
    
    Long create(Tag tag);
    
    void update(Tag tag);
    
    void delete(Long id);
}
