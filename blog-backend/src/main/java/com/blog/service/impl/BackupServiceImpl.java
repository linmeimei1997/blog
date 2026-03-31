package com.blog.service.impl;

import com.blog.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 备份服务实现
 */
@Slf4j
@Service
public class BackupServiceImpl implements BackupService {

    @Value("${app.backup.path:${user.home}/blog/backups}")
    private String backupPath;

    @Value("${app.backup.auto:true}")
    private boolean autoBackup;

    @Value("${app.backup.keep-days:30}")
    private int keepDays;

    @Value("${app.file.upload-path:${user.home}/blog/uploads}")
    private String uploadPath;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(backupPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建备份目录: {}", backupPath);
            }
        } catch (Exception e) {
            log.error("初始化备份目录失败: {}", e.getMessage());
        }
    }

    @Override
    public BackupResult createFullBackup() {
        String backupId = "full_" + LocalDateTime.now().format(DATE_FORMAT);
        Path backupDir = Paths.get(backupPath, backupId);
        
        try {
            Files.createDirectories(backupDir);
            
            // 备份知识库文件
            backupKnowledgeBaseFiles(backupDir.resolve("kb"));
            
            // 备份数据库（简化实现，实际应使用数据库导出工具）
            createDatabaseBackupMarker(backupDir.resolve("db.marker"));
            
            // 打包为 zip
            Path zipFile = Paths.get(backupPath, backupId + ".zip");
            zipDirectory(backupDir, zipFile);
            
            // 删除临时目录
            deleteDirectory(backupDir);
            
            long size = Files.size(zipFile);
            log.info("完整备份完成: {} ({} bytes)", backupId, size);
            
            return BackupResult.success(backupId, zipFile.toString(), size);
            
        } catch (Exception e) {
            log.error("完整备份失败: {}", e.getMessage(), e);
            return BackupResult.error("备份失败: " + e.getMessage());
        }
    }

    @Override
    public BackupResult createIncrementalBackup() {
        // 简化实现：增量备份与全量备份相同
        return createFullBackup();
    }

    @Override
    public BackupResult backupKnowledgeBase() {
        String backupId = "kb_" + LocalDateTime.now().format(DATE_FORMAT);
        Path backupDir = Paths.get(backupPath, backupId);
        
        try {
            Files.createDirectories(backupDir);
            backupKnowledgeBaseFiles(backupDir);
            
            Path zipFile = Paths.get(backupPath, backupId + ".zip");
            zipDirectory(backupDir, zipFile);
            deleteDirectory(backupDir);
            
            long size = Files.size(zipFile);
            log.info("知识库备份完成: {} ({} bytes)", backupId, size);
            
            return BackupResult.success(backupId, zipFile.toString(), size);
            
        } catch (Exception e) {
            log.error("知识库备份失败: {}", e.getMessage());
            return BackupResult.error("知识库备份失败: " + e.getMessage());
        }
    }

    @Override
    public BackupResult backupDatabase() {
        // 简化实现，实际应调用数据库导出命令
        String backupId = "db_" + LocalDateTime.now().format(DATE_FORMAT);
        log.info("数据库备份标记创建: {}", backupId);
        return BackupResult.success(backupId, "db_backup_marker", 0);
    }

    @Override
    public List<BackupInfo> listBackups() {
        try {
            Path path = Paths.get(backupPath);
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            
            return Files.list(path)
                    .filter(p -> p.toString().endsWith(".zip"))
                    .map(this::toBackupInfo)
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取备份列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean restoreBackup(String backupId) {
        // 恢复备份的实现
        log.warn("恢复备份功能需要手动执行: {}", backupId);
        return false;
    }

    @Override
    public boolean deleteBackup(String backupId) {
        try {
            Path zipFile = Paths.get(backupPath, backupId + ".zip");
            return Files.deleteIfExists(zipFile);
        } catch (Exception e) {
            log.error("删除备份失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int cleanupOldBackups(int keepDays) {
        try {
            Path path = Paths.get(backupPath);
            if (!Files.exists(path)) return 0;
            
            LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
            
            List<Path> oldBackups = Files.list(path)
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant()
                                    .isBefore(cutoff.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            
            int count = 0;
            for (Path backup : oldBackups) {
                Files.deleteIfExists(backup);
                count++;
            }
            
            log.info("清理过期备份: {} 个", count);
            return count;
            
        } catch (Exception e) {
            log.error("清理过期备份失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 定时自动备份（每天凌晨2点）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledBackup() {
        if (!autoBackup) return;
        
        log.info("执行定时自动备份...");
        BackupResult result = createFullBackup();
        if (result.isSuccess()) {
            cleanupOldBackups(keepDays);
        }
    }

    private void backupKnowledgeBaseFiles(Path targetDir) throws IOException {
        Path kbDir = Paths.get(uploadPath, "kb");
        if (!Files.exists(kbDir)) return;
        
        Files.createDirectories(targetDir);
        
        Files.walk(kbDir).forEach(source -> {
            try {
                Path target = targetDir.resolve(kbDir.relativize(source));
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception e) {
                log.warn("备份文件失败: {}", source);
            }
        });
    }

    private void createDatabaseBackupMarker(Path markerFile) throws IOException {
        Files.write(markerFile, ("Database backup at " + LocalDateTime.now()).getBytes());
    }

    private void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walk(sourceDir).forEach(path -> {
                try {
                    String zipEntryName = sourceDir.relativize(path).toString();
                    if (Files.isDirectory(path)) {
                        if (!zipEntryName.isEmpty()) {
                            zos.putNextEntry(new ZipEntry(zipEntryName + "/"));
                            zos.closeEntry();
                        }
                    } else {
                        zos.putNextEntry(new ZipEntry(zipEntryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (Exception e) {
                    log.warn("压缩文件失败: {}", path);
                }
            });
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        log.warn("删除文件失败: {}", path);
                    }
                });
    }

    private BackupInfo toBackupInfo(Path path) {
        try {
            BackupInfo info = new BackupInfo();
            String fileName = path.getFileName().toString().replace(".zip", "");
            info.setBackupId(fileName);
            info.setPath(path.toString());
            info.setSize(Files.size(path));
            info.setCreateTime(LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    java.time.ZoneId.systemDefault()));
            info.setStatus("completed");
            
            if (fileName.startsWith("full_")) {
                info.setType("full");
            } else if (fileName.startsWith("kb_")) {
                info.setType("knowledge_base");
            } else if (fileName.startsWith("db_")) {
                info.setType("database");
            } else {
                info.setType("unknown");
            }
            
            return info;
        } catch (Exception e) {
            return null;
        }
    }
}
