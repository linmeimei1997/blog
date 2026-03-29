# 博客系统部署文档

## 版本信息
- **文档版本**: v2.1.0
- **更新日期**: 2026-03-30
- **更新内容**: 新增移动端适配、AI 图文笔记功能

## 服务器信息
- **实例 ID**: a0760e70bc2d48edb80f1635034e865e
- **系统**: 宝塔 Linux 面板 9.2.0 / Docker 部署
- **地域**: 华北 2（北京）
- **数据库**: MySQL 8.0+ (Docker 容器)

---

## 一、环境准备

### 1.1 所需软件版本
| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | Spring Boot 3.x 需要 |
| MySQL | 8.0+ | 生产数据库（开发使用 H2） |
| Nginx | 1.20+ | 反向代理和静态资源 |
| Node.js | 18+ | 前端构建 |

### 1.2 宝塔面板安装软件

登录宝塔面板后，在【软件商店】安装以下软件：

```
1. Nginx 1.24
2. MySQL 8.0
3. PM2 管理器 4.x（用于 Node.js 应用管理）
```

---

## 二、Java环境安装

### 2.1 安装JDK 17

SSH登录服务器，执行以下命令：

```bash
# 安装OpenJDK 17
yum install -y java-17-openjdk java-17-openjdk-devel

# 验证安装
java -version
# 输出: openjdk version "17.x.x"

# 配置环境变量
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk' >> /etc/profile
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile
source /etc/profile
```

---

## 三、MySQL数据库配置

### 3.1 创建数据库

```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户并授权
CREATE USER 'blog'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON blog.* TO 'blog'@'localhost';
FLUSH PRIVILEGES;
```

### 3.2 导入初始数据

```bash
# 上传SQL文件到服务器后导入
mysql -u blog -p blog < /www/wwwroot/blog/sql/schema-mysql.sql
```

---

## 四、后端部署

### 4.1 项目目录结构

```
/www/wwwroot/blog/
├── backend/
│   ├── blog.jar                    # 打包后的jar文件
│   ├── application-prod.yml        # 生产环境配置
│   ├── uploads/                    # 上传文件目录
│   │   ├── avatars/                # 头像
│   │   └── images/                 # 图片
│   └── logs/                       # 日志目录
└── frontend/
    └── dist/                       # 前端构建产物
```

### 4.2 创建生产环境配置

创建 `application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: blog
    password: YourStrongPassword123!
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  # AI 大模型配置
  ai:
    dashscope:
      api-key: ${AI_API_KEY}  # 从环境变量读取
      chat:
        options:
          model: qwen-plus
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

# JWT 配置
jwt:
  secret: YourJwtSecretKeyMustBeVeryLongAndSecure123!@#
  expiration: 86400000

# 日志配置
logging:
  file:
    path: /www/wwwroot/blog/backend/logs
  level:
    com.blog: INFO
    org.springframework: WARN
```

### 4.3 打包后端项目

本地执行：
```bash
cd blog-backend
mvn clean package -DskipTests
```

将 `target/blog-backend-1.0.0.jar` 上传到服务器 `/www/wwwroot/blog/backend/blog.jar`

### 4.4 创建Systemd服务

创建服务文件 `/etc/systemd/system/blog.service`：

```ini
[Unit]
Description=Blog Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=www
Group=www
WorkingDirectory=/www/wwwroot/blog/backend
Environment="AI_API_KEY=sk-your-dashscope-api-key"
Environment="SPRING_PROFILES_ACTIVE=prod"
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /www/wwwroot/blog/backend/blog.jar
ExecStop=/bin/kill -15 $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
# 重载systemd配置
systemctl daemon-reload

# 启动服务
systemctl start blog

# 设置开机自启
systemctl enable blog

# 查看状态
systemctl status blog

# 查看日志
journalctl -u blog -f
```

---

## 五、前端部署

### 5.1 本地构建

创建生产环境配置 `.env.production`：

```
VITE_API_BASE_URL=https://your-domain.com/api
```

执行构建：
```bash
cd blog-frontend
npm install
npm run build
```

构建产物：
- 输出目录：`dist/`
- 入口文件：`index.html`
- 静态资源：`assets/`

### 5.2 功能特性说明

#### PC 端功能
- ✅ 二次元可爱风格表格
- ✅ 水精灵按钮动画效果（查看/编辑/删除）
- ✅ 下拉按钮黄色耳朵装饰
- ✅ 登录页面动漫小人互动
- ✅ 粉色渐变主题
- ✅ 响应式布局

#### 移动端功能（v2.1.0 新增）
- ✅ 移动端专属路由 (`/m/*`)
- ✅ 统一底部导航栏（首页、文章、AI助手、分类）
- ✅ 移动端 AI 助手（支持图片上传）
- ✅ 移动端文章阅读与编辑
- ✅ 移动端分类浏览
- ✅ 移动端个人中心

#### AI 助手功能增强（v2.1.0）
- ✅ 图片上传生成图文笔记
- ✅ 自动生成博客文章
- ✅ 自动保存到知识库
- ✅ 图文混排渲染

### 5.3 上传前端文件

将 `dist` 目录上传到服务器 `/www/wwwroot/blog/frontend/dist/`

---

## 六、Nginx配置

### 6.1 创建Nginx配置

在宝塔面板【网站】->【添加站点】，或手动创建配置文件 `/www/server/panel/vhost/nginx/blog.conf`：

```nginx
# 后端API服务
upstream blog_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com;  # 替换为你的域名
    
    # 开启gzip压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1k;
    
    # 前端静态资源
    location / {
        root /www/wwwroot/blog/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://blog_backend/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # SSE流式响应支持
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
    
    # 上传文件访问
    location /uploads/ {
        alias /www/wwwroot/blog/backend/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /www/wwwroot/blog/frontend/dist;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

### 6.2 测试并重载Nginx

```bash
# 测试配置
nginx -t

# 重载配置
nginx -s reload
```

---

## 七、SSL证书配置（HTTPS）

### 7.1 宝塔面板申请证书

1. 在宝塔面板【网站】找到站点
2. 点击【SSL】->【Let's Encrypt】
3. 申请免费SSL证书

### 7.2 手动配置HTTPS

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /www/server/panel/vhost/cert/your-domain.com/fullchain.pem;
    ssl_certificate_key /www/server/panel/vhost/cert/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # ... 其他配置同上
}

# HTTP跳转HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

---

## 八、防火墙配置

### 8.1 宝塔面板配置

在【安全】页面放行端口：
- 80 (HTTP)
- 443 (HTTPS)
- 22 (SSH)
- 8888 (宝塔面板)

### 8.2 阿里云安全组

在阿里云控制台配置安全组规则：
- 入方向放行 80、443 端口

---

## 九、通义千问API配置

### 9.1 获取API Key

1. 访问 [阿里云DashScope控制台](https://dashscope.console.aliyun.com/)
2. 开通服务并创建API Key

### 9.2 配置环境变量

```bash
# 编辑服务文件
vim /etc/systemd/system/blog.service

# 在[Service]段添加
Environment="AI_API_KEY=sk-your-actual-api-key"

# 重启服务
systemctl daemon-reload
systemctl restart blog
```

---

## 十、日常运维

### 10.1 常用命令

```bash
# 查看服务状态
systemctl status blog

# 重启服务
systemctl restart blog

# 查看日志
tail -f /www/wwwroot/blog/backend/logs/blog.log

# 查看实时日志
journalctl -u blog -f

# 备份数据库
mysqldump -u blog -p blog > /www/backup/blog_$(date +%Y%m%d).sql

# 更新部署
# 1. 上传新的 jar 文件
# 2. systemctl restart blog

# 前端更新
# 1. 本地构建 npm run build
# 2. 上传 dist 目录到服务器
# 3. Nginx 会自动使用新文件
```

### 10.2 日志轮转

创建 `/etc/logrotate.d/blog`：

```
/www/wwwroot/blog/backend/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 www www
}
```

---

## 十一、故障排查

### 11.1 Docker 部署常见问题记录（2026-03-29 更新）

本次 Docker 部署过程中遇到并修复的问题：

#### 1. 数据库表字段缺失

**问题现象**: `Unknown column 'xxx' in 'field list'`

**涉及表和字段**:
| 表名 | 缺失字段 |
|------|---------|
| user | signature |
| article | cover_image, publish_time |
| category | update_time |
| tag | article_count, update_time |
| kb_document | file_name, chunk_count, upload_by, upload_time |
| **缺失表** | kb_chunk, ai_chat_session, ai_chat_message, ai_tool_log, sys_config |

**根本原因**: init.sql 文件不完整，与 MyBatis Mapper 文件不匹配

**解决方案**: 
- 更新 `sql/init.sql`，添加所有缺失的表和字段
- 使用 `INSERT IGNORE` 确保幂等性
- 所有表使用 `utf8mb4 COLLATE utf8mb4_unicode_ci` 字符集

#### 2. 中文乱码问题

**问题现象**: 数据库中的中文显示为乱码（如 `????`）

**根本原因**: 
- MySQL 容器默认字符集为 latin1
- JDBC 连接未指定 UTF-8 编码

**解决方案**:
```yaml
# docker-compose.yml 添加 MySQL 字符集配置
mysql:
  environment:
    LANG: C.UTF-8
  command: >
    --character-set-server=utf8mb4
    --collation-server=utf8mb4_unicode_ci
    --init-connect='SET NAMES utf8mb4'
    --skip-character-set-client-handshake
```

```yaml
# application.yml JDBC URL 使用 UTF-8
url: jdbc:mysql://...?characterEncoding=UTF-8
```

#### 3. 密码校验失败

**问题现象**: 登录时提示"用户名或密码错误"

**根本原因**: 
- 数据库中的密码哈希与明文不匹配
- init.sql 中的密码哈希 `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iDJMlqPSgYRCXjpJRJnhM4R5jLQu` 对应的明文不是 `admin123`

**解决方案**:
- 使用正确的 BCrypt 哈希: `$2a$10$I2E5Mjt1CPHUmty0Mwxgn.93FmQtjoXppev.oGO4dkq9ss9Dqx0Z.`
- 对应明文密码: `admin123`

#### 4. JWT 配置缺失

**问题现象**: 后端启动报错 `Could not resolve placeholder 'app.jwt.secret'`

**根本原因**: `application.yml` 的 prod 配置缺少 `app.jwt` 配置节

**解决方案**: 在 prod 配置中添加:
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:blog-jwt-secret-key-2024-very-long-and-secure-key-for-production}
    expiration: 86400000
```

#### 5. AI 配置缺失

**问题现象**: 后端启动报错 `DashScope API key must be set`

**根本原因**: `application.yml` 的 prod 配置缺少 `spring.ai.dashscope` 配置

**解决方案**: 在 prod 配置中添加:
```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_API_KEY:}
```

#### 6. MySQL 8.0 连接问题

**问题现象**: `Failed to obtain JDBC Connection`

**根本原因**: MySQL 8.0 默认使用 `caching_sha2_password` 认证插件

**解决方案**: JDBC URL 添加 `allowPublicKeyRetrieval=true`
```
jdbc:mysql://...?allowPublicKeyRetrieval=true
```

### 11.2 传统部署常见问题

| 问题 | 可能原因 | 解决方案 |
|------|---------|----------|
| 502 Bad Gateway | 后端未启动 | `systemctl start blog` |
| 数据库连接失败 | 密码错误/服务未启动 | 检查 MySQL 状态和配置 |
| AI 助手无响应 | API Key 未配置 | 配置环境变量 |
| 图片无法访问 | Nginx 配置错误 | 检查/uploads/路径配置 |
| 登录失败 | JWT 密钥不匹配 | 检查 jwt.secret 配置 |
| 按钮无动画效果 | 浏览器缓存 | 清除缓存或 Ctrl+Shift+R 刷新 |
| 前端页面不更新 | 静态资源缓存 | Nginx 配置中添加版本号或时间戳 |

### 11.2 检查端口占用

```bash
netstat -tlnp | grep :8080
```

### 11.3 检查服务日志

```bash
# 后端日志
tail -100 /www/wwwroot/blog/backend/logs/blog.log

# Nginx错误日志
tail -100 /www/wwwlogs/error.log
```

---

## 十二、性能优化建议

### 12.1 JVM参数调优

```bash
# 编辑 /etc/systemd/system/blog.service
ExecStart=/usr/bin/java \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/www/wwwroot/blog/backend/logs/heapdump.hprof \
  -jar /www/wwwroot/blog/backend/blog.jar
```

### 12.2 MySQL优化

```sql
-- 添加索引
CREATE INDEX idx_article_create_time ON article(create_time);
CREATE INDEX idx_article_status ON article(status);
```

### 12.3 Redis缓存（可选）

```yaml
# application-prod.yml 添加
spring:
  redis:
    host: localhost
    port: 6379
    password: your-redis-password
```

---

## 十三、前端特性详解

### 13.1 水精灵按钮样式

所有操作按钮已统一为可爱的水精灵风格：

**类名说明：**
- `.water-spirit-btn` - 基础按钮样式（带耳朵动画）
- `.water-spirit-primary` - 蓝色查看按钮
- `.water-spirit-default` - 黄色编辑按钮
- `.water-spirit-danger` - 红色删除按钮

**动画效果：**
- 耳朵摇摆（earWiggle）
- hover 上浮抖动（floatShake）
- 点击下沉缩放

**使用示例：**
```html
<!-- 查看按钮 -->
<el-button class="water-spirit-btn water-spirit-primary">查看</el-button>

<!-- 编辑按钮 -->
<el-button class="water-spirit-btn water-spirit-default">编辑</el-button>

<!-- 删除按钮 -->
<el-button class="water-spirit-btn water-spirit-danger">删除</el-button>
```

### 13.2 全局样式文件

水精灵按钮样式已提取到全局文件：
- 文件位置：`src/styles/water-spirit-buttons.scss`
- 自动在 `main.js` 中引入
- 所有页面无需重复定义

---

## 十四、Docker 部署（推荐）

### 14.1 快速开始

使用 Docker Compose 一键部署（已解决上述所有问题）：

```bash
# 1. 克隆项目并进入目录
cd /www/blog

# 2. 创建 .env 文件
cat > .env <<'EOF'
MYSQL_ROOT_PASSWORD=YourStrongRootPassword123!
MYSQL_PASSWORD=YourStrongBlogPassword123!
AI_API_KEY=sk-your-dashscope-api-key
JWT_SECRET=YourVeryLongAndSecureJwtSecretKey123!@#
EOF

# 3. 启动所有服务
docker compose up -d

# 4. 初始化数据库
sleep 20
docker exec -i blog-mysql mysql -u root -p'YourStrongRootPassword123!' blog < sql/init.sql

# 5. 检查状态
docker compose ps
```

### 14.2 Docker 部署检查清单

- [ ] `.env` 文件已创建并配置正确
- [ ] `sql/init.sql` 已更新到最新版本
- [ ] `docker-compose.yml` 已更新字符集配置
- [ ] `application.yml` 已更新 prod 配置
- [ ] Docker 和 Docker Compose 已安装
- [ ] 端口 80/8080/3306 未被占用
- [ ] 容器状态正常 (healthy)
- [ ] 数据库初始化成功
- [ ] 登录功能正常 (admin / admin123)
- [ ] 中文显示正常无乱码
- [ ] AI 助手功能正常

### 14.3 相关文件说明

| 文件 | 用途 | 关键配置 |
|------|------|---------|
| `docker-compose.yml` | Docker 编排 | MySQL 字符集、环境变量 |
| `sql/init.sql` | 数据库初始化 | 所有表结构、默认数据 |
| `.env` | 环境变量 | 密码、API Key、JWT Secret |
| `blog-backend/src/main/resources/application.yml` | 后端配置 | JDBC URL、JWT、AI 配置 |
| `宝塔 Docker 部署手顺.md` | 详细部署文档 | 完整步骤和常见问题 |

---

## 十五、部署检查清单

### 传统部署
- [ ] JDK 17 已安装
- [ ] MySQL 已安装并创建数据库
- [ ] 后端 jar 包已上传
- [ ] Systemd 服务已配置并启动
- [ ] 前端 dist 已上传
- [ ] Nginx 配置正确
- [ ] 域名已解析到服务器
- [ ] SSL 证书已配置
- [ ] 阿里云 API Key 已配置
- [ ] 防火墙端口已放行
- [ ] 服务可正常访问
- [ ] 按钮动画效果正常
- [ ] AI 助手功能正常

### Docker 部署
- [ ] Docker 和 Docker Compose 已安装
- [ ] `.env` 文件已正确配置
- [ ] 所有配置文件已更新到最新版本
- [ ] 数据库初始化成功
- [ ] 登录功能正常
- [ ] 中文显示正常
- [ ] 各功能模块正常

---

## 联系方式

如有问题，请检查日志文件或联系运维人员。

---

## 更新日志

### v2.1.0 (2026-03-30)
- 新增移动端完整适配
  - 移动端首页 `/m/home`
  - 移动端文章列表 `/m/articles`
  - 移动端文章详情 `/m/article/:id`
  - 移动端 AI 助手 `/m/ai`（支持图片上传）
  - 移动端分类 `/m/categories`
  - 移动端编辑器 `/m/editor/:id`
  - 移动端个人中心 `/m/profile`
  - 统一底部导航栏
- AI 助手功能增强
  - 支持上传图片
  - 图文笔记自动生成
  - 博客文章自动保存到知识库
- UI 一致性优化
  - 移动端导航栏统一
  - 图片渲染优化（支持相对路径）

### v2.0 (2026-03-29)
- 添加 Docker 部署支持
- 修复数据库字段缺失问题
- 修复中文乱码问题
- 修复密码校验失败问题
- 修复 JWT 和 AI 配置缺失问题
- 添加 MySQL 8.0 连接支持
- 统一使用 `utf8mb4` 字符集
- 更新默认密码为 admin / admin123

### v1.0 (初始版本)
- 传统部署方式（Systemd + Nginx）
- 宝塔面板部署支持
