package com.blog.mapper;

import com.blog.entity.AiChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AI对话消息Mapper
 */
@Mapper
public interface AiChatMessageMapper {
    
    AiChatMessage selectById(@Param("id") Long id);
    
    List<AiChatMessage> selectBySessionId(@Param("sessionId") String sessionId);
    
    List<AiChatMessage> selectRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") Integer limit);
    
    int insert(AiChatMessage message);
    
    int deleteBySessionId(@Param("sessionId") String sessionId);
    
    int countBySessionId(@Param("sessionId") String sessionId);
}
