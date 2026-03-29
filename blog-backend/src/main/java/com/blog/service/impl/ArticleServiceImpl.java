package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQuery;
import com.blog.dto.PageResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.ArticleService;
import com.blog.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;

    @Override
    public Article getById(Long id) {
        Article article = articleMapper.selectById(id);
        if (article != null) {
            List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(id);
            article.setTags(tagIds.stream()
                    .map(tagMapper::selectById)
                    .collect(Collectors.toList()));
        }
        return article;
    }

    @Override
    public PageResult<Article> list(ArticleQuery query) {
        List<Article> list = articleMapper.selectList(query);
        Long total = articleMapper.countList(query);
        
        // 填充标签信息
        for (Article article : list) {
            List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(article.getId());
            article.setTags(tagIds.stream()
                    .map(tagMapper::selectById)
                    .collect(Collectors.toList()));
        }
        
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ArticleDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setCategoryId(dto.getCategoryId());
        article.setAuthorId(SecurityUtils.getCurrentUserId());
        article.setStatus(dto.getStatus());
        
        if (dto.getStatus() != null && dto.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }
        
        articleMapper.insert(article);
        
        // 保存标签关联
        saveArticleTags(article.getId(), dto.getTagIds());
        
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ArticleDTO dto) {
        Article article = new Article();
        article.setId(dto.getId());
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary());
        article.setContent(dto.getContent());
        article.setCoverImage(dto.getCoverImage());
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus());
        
        if (dto.getStatus() != null && dto.getStatus() == 1) {
            Article old = articleMapper.selectById(dto.getId());
            if (old != null && old.getPublishTime() == null) {
                article.setPublishTime(LocalDateTime.now());
            }
        }
        
        articleMapper.update(article);
        
        // 更新标签关联
        articleTagMapper.deleteByArticleId(dto.getId());
        saveArticleTags(dto.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        articleTagMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }

    @Override
    public void incrementViewCount(Long id) {
        articleMapper.incrementViewCount(id);
    }

    @Override
    public List<Article> getLatest(Integer limit) {
        return articleMapper.selectLatest(limit);
    }

    @Override
    public List<Article> getByCategory(Long categoryId) {
        return articleMapper.selectByCategory(categoryId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Article article) {
        if (article.getCreateTime() == null) {
            article.setCreateTime(LocalDateTime.now());
        }
        if (article.getUpdateTime() == null) {
            article.setUpdateTime(LocalDateTime.now());
        }
        if (article.getPublishTime() == null && article.getStatus() != null && article.getStatus() == 1) {
            article.setPublishTime(LocalDateTime.now());
        }
        articleMapper.insert(article);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntity(Article article) {
        article.setUpdateTime(LocalDateTime.now());
        articleMapper.update(article);
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return;
        }
        
        List<ArticleTag> articleTags = new ArrayList<>();
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTags.add(at);
        }
        
        articleTagMapper.insertBatch(articleTags);
    }
}
