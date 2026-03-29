package com.blog.mapper;

import com.blog.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 知识库文档Mapper
 */
@Mapper
public interface KbDocumentMapper {
    
    KbDocument selectById(@Param("id") Long id);
    
    List<KbDocument> selectList(@Param("keyword") String keyword);
    
    int insert(KbDocument document);
    
    int update(KbDocument document);
    
    int deleteById(@Param("id") Long id);
    
    int updateChunkCount(@Param("id") Long id, @Param("chunkCount") Integer chunkCount);
}
