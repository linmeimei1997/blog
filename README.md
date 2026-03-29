# 博客 & AI 助手系统

一套完整的前后端分离的博客与 AI 助手系统，采用 Spring Boot 3 + Vue3 技术栈，支持 PC 端和移动端响应式访问。

**版本**: v2.1.0
**更新日期**: 2026-03-30

## 项目结构

```
blog/
├── blog-backend/          # 后端项目 (Spring Boot)
│   ├── src/main/java/     # Java 源代码
│   ├── src/main/resources/# 配置文件
│   └── pom.xml            # Maven 配置
├── blog-frontend/         # 前端项目 (Vue3)
│   ├── src/               # 源代码
│   ├── package.json       # NPM 配置
│   └── vite.config.js     # Vite 配置
└── README.md              # 项目说明
```

## 技术栈

### 后端
- Spring Boot 3.4.5
- Spring Security + JWT
- MyBatis 3.x
- MySQL 5.7+ / H2 (开发测试)
- Spring AI Alibaba (通义千问)
- Logback 日志
- Maven

### 前端
- Vue 3 + Vite
- Element Plus UI
- Pinia 状态管理
- Vue Router
- Axios
- md-editor-v3 (Markdown 编辑器)

## 快速开始

### 1. 启动后端

```bash
# 进入后端目录
cd blog-backend

# 编译项目
mvn clean install

# 运行项目
mvn spring-boot:run
```

后端服务将启动在 `http://localhost:8080`

### 2. 启动前端

```bash
# 进入前端目录
cd blog-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将启动在 `http://localhost:5173`

### 3. 访问系统

打开浏览器访问: http://localhost:5173

## 测试账号

- 用户名: `admin`
- 密码: `admin123`

## AI 配置

### 配置通义千问 API Key

1. 访问阿里云百炼平台: https://bailian.console.aliyun.com/
2. 创建 API Key
3. 修改后端配置文件 `blog-backend/src/main/resources/application.yml`:

```yaml
spring:
  ai:
    dashscope:
      api-key: your-api-key-here
```

或在启动时设置环境变量:
```bash
set DASHSCOPE_API_KEY=your-api-key-here
mvn spring-boot:run
```

## 功能特性

### 博客管理
- 文章 CRUD 操作
- Markdown 编辑器
- 分类/标签管理
- 文章搜索

### 知识库
- 文档上传 (txt, md, pdf, docx)
- 文档解析与分块
- 内容检索

### AI 助手
- 流式对话
- 多轮会话管理
- RAG 知识库问答
- ToolCalling 工具调用
    - 获取文章列表
    - 获取文章详情
    - 搜索知识库
    - 文档总结
- **图片上传与图文笔记**
    - 支持上传图片
    - AI 自动生成图文混排的读书笔记
    - AI 自动生成博客文章
    - 自动保存到知识库

### 移动端适配
- 响应式移动端界面
- 移动端专属路由 (`/m/*`)
- 移动端底部导航栏（首页、文章、AI助手、分类）
- 移动端 AI 助手完整功能
- 移动端文章阅读与编辑

## 数据库

### 开发环境 (H2)
默认使用 H2 内存数据库，数据在重启后丢失。

### 生产环境 (MySQL)

1. 创建数据库:
```sql
CREATE DATABASE blogdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 修改 `application.yml` 激活 prod 配置:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

或在 `application.yml` 中修改:
```yaml
spring:
  profiles:
    active: prod
```

3. 配置数据库连接信息 (环境变量):
```bash
set MYSQL_USERNAME=root
set MYSQL_PASSWORD=your-password
```

## API 文档

后端 API 统一前缀: `/api`

主要接口:
- `POST /api/auth/login` - 登录
- `GET /api/article/list` - 文章列表
- `GET /api/article/{id}` - 文章详情
- `GET /api/ai/chat/stream` - AI 流式对话 (SSE)
- `GET /api/kb/list` - 知识库列表

## 部署

### 后端打包
```bash
cd blog-backend
mvn clean package
# 生成 target/blog-backend-1.0.0.jar
```

### 前端打包
```bash
cd blog-frontend
npm run build
# 生成 dist/ 目录
```

### Docker 部署 (可选)
```bash
# 构建镜像
docker build -t blog-backend ./blog-backend
docker build -t blog-frontend ./blog-frontend

# 运行
docker-compose up -d
```

## 日志

日志文件位置: `~/blog/logs/`
- `blog.log` - 应用日志
- `error.log` - 错误日志
- `ai.log` - AI 对话日志

## 许可证

MIT License

---

## 更新日志

### v2.1.0 (2026-03-30)
- 新增移动端完整适配
  - 移动端首页、文章列表、文章详情
  - 移动端 AI 助手（支持图片上传）
  - 移动端分类页面
  - 移动端个人中心与编辑器
  - 统一底部导航栏（首页、文章、AI助手、分类）
- AI 助手功能增强
  - 支持上传图片生成图文笔记
  - 自动生成博客文章并保存到知识库
  - 自动生成读书笔记
- UI 一致性优化
  - 移动端页面导航栏统一
  - 图片渲染优化

### v2.0.0 (2026-03-29)
- Docker 部署支持
- 数据库字段完善
- 中文乱码修复
- JWT 和 AI 配置完善

### v1.0.0 (初始版本)
- 基础博客功能
- AI 对话功能
- 知识库管理
