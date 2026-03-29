package com.blog.dto;

import lombok.Data;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateProfileRequest {
    
    private String nickname;
    
    private String email;
    
    private String signature;
}
