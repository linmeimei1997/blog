package com.blog.service;

import com.blog.dto.LoginRequest;
import com.blog.dto.LoginResponse;
import com.blog.dto.UpdateProfileRequest;
import com.blog.dto.UserDTO;
import com.blog.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    LoginResponse login(LoginRequest request);
    
    User getCurrentUser();
    
    User getById(Long id);
    
    User getByUsername(String username);
    
    /**
     * 获取用户资料
     */
    UserDTO getProfile(Long userId);
    
    /**
     * 更新用户资料
     */
    UserDTO updateProfile(Long userId, UpdateProfileRequest request);
    
    /**
     * 更新用户头像
     */
    UserDTO updateAvatar(Long userId, String avatarUrl);
    
    /**
     * 修改密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 用户注册
     */
    LoginResponse register(String username, String password, String nickname);
}
