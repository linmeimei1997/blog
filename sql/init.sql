-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `signature` VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='用户表';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content` TEXT COMMENT '内容',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类 ID',
  `author_id` BIGINT DEFAULT NULL COMMENT '作者 ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-发布，0-草稿',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author` (`author_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='文章表';

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父分类 ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `article_count` INT DEFAULT 0 COMMENT '文章数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='标签表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS `article_tag` (
  `article_id` BIGINT NOT NULL COMMENT '文章 ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签 ID',
  PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  `file_type` VARCHAR(20) DEFAULT NULL COMMENT '文件类型',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小',
  `file_path` VARCHAR(255) DEFAULT NULL COMMENT '文件路径',
  `content` TEXT COMMENT '内容',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `chunk_count` INT DEFAULT 0 COMMENT '分块数量',
  `upload_by` BIGINT DEFAULT NULL COMMENT '上传者 ID',
  `upload_time` DATETIME DEFAULT NULL COMMENT '上传时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='知识库文档表';

-- AI 聊天会话表
CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `message_count` INT DEFAULT 0 COMMENT '消息数量',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='AI 聊天会话表';

-- AI 聊天消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
  `content` TEXT COMMENT '消息内容',
  `message_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型',
  `tools_used` VARCHAR(500) DEFAULT NULL COMMENT '使用的工具',
  `tokens` INT DEFAULT NULL COMMENT 'Token 数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='AI 聊天消息表';

-- AI 工具调用日志表
CREATE TABLE IF NOT EXISTS `ai_tool_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '会话 ID',
  `message_id` BIGINT DEFAULT NULL COMMENT '消息 ID',
  `tool_name` VARCHAR(50) NOT NULL COMMENT '工具名称',
  `tool_input` TEXT COMMENT '工具输入',
  `tool_output` TEXT COMMENT '工具输出',
  `duration` INT DEFAULT NULL COMMENT '执行时长(ms)',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-成功，0-失败',
  `error_msg` TEXT COMMENT '错误信息',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='AI 工具调用日志表';

-- 知识库文档分块表
CREATE TABLE IF NOT EXISTS `kb_chunk` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `document_id` BIGINT NOT NULL COMMENT '文档 ID',
  `content` TEXT COMMENT '分块内容',
  `vector_id` VARCHAR(64) DEFAULT NULL COMMENT '向量 ID',
  `chunk_index` INT DEFAULT 0 COMMENT '分块序号',
  `start_pos` INT DEFAULT NULL COMMENT '开始位置',
  `end_pos` INT DEFAULT NULL COMMENT '结束位置',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='知识库文档分块表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) DEFAULT NULL COMMENT '配置值',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认数据（使用 INSERT IGNORE 避免重复执行时报错）
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `nickname`, `signature`, `status`) 
VALUES ('admin', '$2a$10$I2E5Mjt1CPHUmty0Mwxgn.93FmQtjoXppev.oGO4dkq9ss9Dqx0Z.', 'admin@example.com', '管理员', NULL, 1);

INSERT IGNORE INTO `category` (`name`, `description`, `sort_order`) VALUES 
('技术文章', '分享技术心得和教程', 1),
('生活随笔', '记录生活中的点点滴滴', 2);

INSERT IGNORE INTO `tag` (`name`) VALUES 
('Java'),
('Spring Boot'),
('Vue.js'),
('Docker');
