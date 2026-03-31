package com.blog.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备份服务接口
 */
public interface BackupService {
    
    /**
     * 创建完整备份
     */
    BackupResult createFullBackup();
    
    /**
     * 创建增量备份
     */
    BackupResult createIncrementalBackup();
    
    /**
     * 备份知识库数据
     */
    BackupResult backupKnowledgeBase();
    
    /**
     * 备份数据库
     */
    BackupResult backupDatabase();
    
    /**
     * 获取备份列表
     */
    List<BackupInfo> listBackups();
    
    /**
     * 恢复备份
     */
    boolean restoreBackup(String backupId);
    
    /**
     * 删除备份
     */
    boolean deleteBackup(String backupId);
    
    /**
     * 清理过期备份
     */
    int cleanupOldBackups(int keepDays);
    
    /**
     * 备份信息
     */
    class BackupInfo {
        private String backupId;
        private String type;
        private long size;
        private LocalDateTime createTime;
        private String status;
        private String path;
        
        // Getters and Setters
        public String getBackupId() { return backupId; }
        public void setBackupId(String backupId) { this.backupId = backupId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
    
    /**
     * 备份结果
     */
    class BackupResult {
        private boolean success;
        private String backupId;
        private String message;
        private long size;
        private String path;
        
        public static BackupResult success(String backupId, String path, long size) {
            BackupResult result = new BackupResult();
            result.success = true;
            result.backupId = backupId;
            result.path = path;
            result.size = size;
            result.message = "备份成功";
            return result;
        }
        
        public static BackupResult error(String message) {
            BackupResult result = new BackupResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getBackupId() { return backupId; }
        public String getMessage() { return message; }
        public long getSize() { return size; }
        public String getPath() { return path; }
    }
}
