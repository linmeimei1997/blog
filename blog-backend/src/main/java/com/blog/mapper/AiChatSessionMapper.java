package com.blog.mapper;

import com.blog.entity.AiChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AI对话会话Mapper
 */
@Mapper
public interface AiChatSessionMapper {
    
    AiChatSession selectBySessionId(@Param("sessionId") String sessionId);
    
    List<AiChatSession> selectByUserId(@Param("userId") Long userId);
    
    int insert(AiChatSession session);
    
    int update(AiChatSession session);
    
    int updateMessageCount(@Param("sessionId") String sessionId);
    
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
