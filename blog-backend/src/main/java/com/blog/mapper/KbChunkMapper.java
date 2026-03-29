package com.blog.mapper;

import com.blog.entity.KbChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 知识库文档分块Mapper
 */
@Mapper
public interface KbChunkMapper {
    
    int insert(KbChunk chunk);
    
    int insertBatch(@Param("list") List<KbChunk> list);
    
    List<KbChunk> selectByDocumentId(@Param("documentId") Long documentId);
    
    List<KbChunk> searchByKeyword(@Param("keyword") String keyword);
    
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
