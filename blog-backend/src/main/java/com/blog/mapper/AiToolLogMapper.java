package com.blog.mapper;

import com.blog.entity.AiToolLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * AI工具调用日志Mapper
 */
@Mapper
public interface AiToolLogMapper {
    
    int insert(AiToolLog log);
    
    List<AiToolLog> selectRecent(int limit);
}
