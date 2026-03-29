package com.blog.controller;

import com.blog.dto.ChangePasswordRequest;
import com.blog.dto.Result;
import com.blog.dto.UpdateProfileRequest;
import com.blog.dto.UserDTO;
import com.blog.service.UserService;
import com.blog.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户资料
     */
    @GetMapping("/profile")
    public Result<UserDTO> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        UserDTO userDTO = userService.getProfile(userId);
        return Result.success(userDTO);
    }

    /**
     * 更新用户资料
     */
    @PutMapping("/profile")
    public Result<UserDTO> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        UserDTO userDTO = userService.updateProfile(userId, request);
        return Result.success(userDTO);
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        log.info("用户 {} 上传头像, 文件名: {}, 大小: {}", userId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 检查文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }
            
            // 检查文件大小 (最大 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return Result.error("图片大小不能超过 2MB");
            }
            
            // 保存图片
            String avatarUrl = saveAvatar(file);
            
            // 更新用户头像
            userService.updateAvatar(userId, avatarUrl);
            
            log.info("头像上传成功, URL: {}", avatarUrl);
            return Result.success(avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录");
        }
        
        try {
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private String saveAvatar(MultipartFile file) throws Exception {
        // 使用项目目录下的绝对路径
        String projectDir = System.getProperty("user.dir");
        String uploadDir = projectDir + File.separator + "uploads" + File.separator + "avatars";
        File dir = new File(uploadDir);
        
        log.info("头像上传目录: {}", uploadDir);
        
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new RuntimeException("无法创建上传目录: " + uploadDir);
            }
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        File dest = new File(dir, fileName);
        
        log.info("保存头像到: {}", dest.getAbsolutePath());
        
        file.transferTo(dest);
        
        // 返回相对路径，前端会通过代理访问
        return "/uploads/avatars/" + fileName;
    }
}
