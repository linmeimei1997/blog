package com.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上传图片的静态资源访问
        String projectDir = System.getProperty("user.dir");
        String uploadPath = projectDir + java.io.File.separator + "uploads" + java.io.File.separator;
        
        // Windows 路径需要转换为 file URL 格式
        String resourceLocation = "file:" + uploadPath.replace("\\", "/");
        
        System.out.println("静态资源路径: " + resourceLocation);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
        
        // 配置头像目录
        String avatarPath = projectDir + java.io.File.separator + "uploads" + java.io.File.separator + "avatars" + java.io.File.separator;
        String avatarLocation = "file:" + avatarPath.replace("\\", "/");
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(avatarLocation);
    }
}
