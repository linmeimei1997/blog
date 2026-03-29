package com.blog.service.impl;

import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public Tag getById(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    public List<Tag> listAll() {
        return tagMapper.selectAll();
    }

    @Override
    public List<Tag> getByArticleId(Long articleId) {
        return tagMapper.selectByArticleId(articleId);
    }

    @Override
    public Long create(Tag tag) {
        Tag exist = tagMapper.selectByName(tag.getName());
        if (exist != null) {
            return exist.getId();
        }
        tagMapper.insert(tag);
        return tag.getId();
    }

    @Override
    public void update(Tag tag) {
        tagMapper.update(tag);
    }

    @Override
    public void delete(Long id) {
        tagMapper.deleteById(id);
    }
}
