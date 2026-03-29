-- H2 数据库建表脚本

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    avatar VARCHAR(255),
    signature VARCHAR(500),
    status INT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    article_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary TEXT,
    content LONGTEXT,
    cover_image VARCHAR(255),
    category_id BIGINT,
    author_id BIGINT,
    status INT DEFAULT 0 COMMENT '0-草稿 1-已发布',
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    publish_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id),
    FOREIGN KEY (author_id) REFERENCES `user`(id)
);

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS article_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE (article_id, tag_id)
);

-- 知识库文档表
CREATE TABLE IF NOT EXISTS kb_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT,
    file_path VARCHAR(500),
    content LONGTEXT,
    status INT DEFAULT 1 COMMENT '0-处理中 1-已完成 2-失败',
    chunk_count INT DEFAULT 0,
    upload_by BIGINT,
    upload_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (upload_by) REFERENCES `user`(id)
);

-- 知识库文档分块表
CREATE TABLE IF NOT EXISTS kb_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    content TEXT,
    vector_id VARCHAR(100),
    chunk_index INT,
    start_pos INT,
    end_pos INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE
);

-- AI 对话会话表
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT,
    title VARCHAR(200),
    message_count INT DEFAULT 0,
    last_message_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES `user`(id)
);

-- AI 对话消息表
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content LONGTEXT,
    message_type VARCHAR(20) DEFAULT 'text',
    tools_used VARCHAR(500),
    tokens INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES ai_chat_session(session_id) ON DELETE CASCADE
);

-- AI 工具调用日志表
CREATE TABLE IF NOT EXISTS ai_tool_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),
    message_id VARCHAR(100),
    tool_name VARCHAR(100) NOT NULL,
    tool_input TEXT,
    tool_output TEXT,
    duration BIGINT COMMENT '执行耗时ms',
    status INT DEFAULT 1 COMMENT '0-失败 1-成功',
    error_msg TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 系统配置表
CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 文章图片表（用于图文混排）
CREATE TABLE IF NOT EXISTS article_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT,
    image_url VARCHAR(500) NOT NULL,
    image_desc VARCHAR(255),
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (article_id) REFERENCES article(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_article_category ON article(category_id);
CREATE INDEX IF NOT EXISTS idx_article_author ON article(author_id);
CREATE INDEX IF NOT EXISTS idx_article_status ON article(status);
CREATE INDEX IF NOT EXISTS idx_article_create_time ON article(create_time);
CREATE INDEX IF NOT EXISTS idx_article_tag_article ON article_tag(article_id);
CREATE INDEX IF NOT EXISTS idx_article_tag_tag ON article_tag(tag_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_document ON kb_chunk(document_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_session_user ON ai_chat_session(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_message_session ON ai_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_article_image_article ON article_image(article_id);
