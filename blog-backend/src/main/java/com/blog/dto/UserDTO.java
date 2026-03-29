package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料DTO
 */
@Data
public class UserDTO {
    
    private Long id;
    
    private String username;
    
    private String nickname;
    
    private String avatar;
    
    private String email;
    
    private String signature;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
