package com.blog.mapper;

import com.blog.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统配置Mapper
 */
@Mapper
public interface SysConfigMapper {
    
    SysConfig selectByKey(@Param("configKey") String configKey);
    
    List<SysConfig> selectAll();
    
    int insert(SysConfig config);
    
    int update(SysConfig config);
}
