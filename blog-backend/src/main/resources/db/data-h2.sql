-- H2 数据库初始数据

-- 初始化用户 (密码: admin123)
-- 使用 BCrypt 加密: admin123
INSERT INTO `user` (username, password, nickname, email, avatar, status) VALUES
('admin', '$2a$10$kk85ZA6f1u41lngDQuHLw.l1gE2YacLvL8uWU67LYj5HQkDLrTQBO', '管理员', 'admin@blog.com', 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png', 1);

-- 初始化分类
INSERT INTO category (name, description, sort_order) VALUES
('技术', '编程技术相关文章', 1),
('生活', '日常生活随笔', 2),
('读书笔记', '阅读书籍的心得体会', 3),
('AI人工智能', '人工智能相关技术', 4);

-- 初始化标签
INSERT INTO tag (name, article_count) VALUES
('Java', 0),
('Spring Boot', 0),
('Vue.js', 0),
('MySQL', 0),
('AI', 0),
('机器学习', 0),
('生活感悟', 0),
('读书笔记', 0);

-- 初始化示例文章
INSERT INTO article (title, summary, content, category_id, author_id, status, view_count, like_count, publish_time) VALUES
('欢迎使用博客系统', '这是一个基于 Spring Boot + Vue3 的博客系统', 
'# 欢迎使用博客系统

这是一个功能完善的博客系统，包含以下特性：

## 功能特点

- 文章管理：支持 Markdown 编辑器
- 分类标签：灵活的文章组织方式
- 知识库：支持文档上传和 RAG 检索
- AI 助手：智能对话和工具调用

## 技术栈

### 后端
- Spring Boot 3.4.5
- MyBatis 3.x
- MySQL / H2
- JWT 认证

### 前端
- Vue 3 + Vite
- Element Plus
- Pinia

开始你的创作之旅吧！', 
1, 1, 1, 100, 10, CURRENT_TIMESTAMP);

-- 文章标签关联
INSERT INTO article_tag (article_id, tag_id) VALUES
(1, 1),
(1, 2);

-- 更新标签文章数
UPDATE tag SET article_count = 1 WHERE id IN (1, 2);

-- 系统配置
INSERT INTO sys_config (config_key, config_value, description) VALUES
('site.name', '我的博客', '网站名称'),
('site.description', '一个基于 Spring Boot + Vue3 的博客系统', '网站描述'),
('ai.enabled', 'true', '是否启用 AI 功能'),
('ai.model', 'qwen-turbo', 'AI 模型');
