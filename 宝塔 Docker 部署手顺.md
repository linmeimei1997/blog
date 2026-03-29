# 宝塔 Linux 面板 Docker 部署手顺

## 服务器信息
- **实例 ID**: a0760e70bc2d48edb80f1635034e865e
- **实例名称**: 宝塔 Linux 面板-qzqp
- **地域**: 华北 2（北京）
- **镜像**: 宝塔 Linux 面板 阿里云专享版 9.2.0

---

## 目录

1. [环境准备](#一环境准备)
2. [安装 Docker](#二安装-docker)
3. [安装 Docker Compose](#三安装-docker-compose)
4. [项目文件准备](#四项目文件准备)
5. [配置环境变量](#五配置环境变量)
6. [构建并启动容器](#六构建并启动容器)
7. [数据库初始化](#七数据库初始化)
8. [验证与访问](#八验证与访问)
9. [日常运维管理](#九日常运维管理)
10. [常见问题排查](#十常见问题排查)

---

## 一、环境准备

### 1.1 登录宝塔面板

1. 打开浏览器，访问宝塔面板地址
   ```
   http://<您的服务器公网 IP>:8888
   ```
2. 输入宝塔面板账号密码登录

### 1.2 安装系统插件

在宝塔面板左侧菜单，点击「软件商店」，搜索并安装以下插件：

| 插件名称 | 说明 | 建议版本 |
|---------|------|---------|
| Docker | 容器运行环境 | 最新版 |
| MySQL | 数据库（可选，如使用 Docker 则不装） | 5.7+ |
| Nginx | Web 服务器（可选，如使用 Docker 则不装） | 1.20+ |

**注意**: 如果使用 Docker Compose 统一部署，建议只安装 Docker，其他服务全部容器化。

---

## 二、安装 Docker

### 方法一：通过宝塔面板安装（推荐）

1. 在宝塔面板点击「软件商店」
2. 搜索 "Docker"
3. 点击「安装」按钮
4. 等待安装完成（约 3-5 分钟）

### 方法二：通过 SSH 命令行安装

1. 使用 SSH 工具连接服务器（如 Xshell、Putty）
   ```bash
   ssh root@<您的服务器 IP>
   ```

2. 执行以下命令安装 Docker：
   ```bash
   # 更新系统包
   yum update -y
   
   # 安装依赖工具
   yum install -y yum-utils device-mapper-persistent-data lvm2
   
   # 添加 Docker 官方仓库
   yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
   
   # 安装 Docker CE
   yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
   
   # 启动 Docker
   systemctl start docker
   systemctl enable docker
   
   # 验证安装
   docker --version
   docker compose version
   ```

3. 配置 Docker 国内镜像加速（重要！）
   ```bash
   cat > /etc/docker/daemon.json <<EOF
   {
     "registry-mirrors": [
       "https://docker.mirrors.ustc.edu.cn",
       "https://registry.docker-cn.com"
     ]
   }
   EOF
   
   # 重启 Docker
   systemctl daemon-reload
   systemctl restart docker
   ```

---

## 三、安装 Docker Compose

### 3.1 检查是否已安装

```bash
docker compose version
```

如果已安装（版本 2.0+），可跳过此步骤。

### 3.2 手动安装（如未安装）

```bash
# 下载 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 添加执行权限
chmod +x /usr/local/bin/docker-compose

# 创建软链接
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 验证安装
docker-compose --version
```

---

## 四、项目文件准备

### 4.1 上传项目到服务器

#### 方法一：通过 Git 克隆（推荐）

```bash
# 创建项目目录
mkdir -p /www/blog
cd /www/blog

# 克隆项目代码（替换为您的 Git 仓库地址）
git clone <您的 Git 仓库地址> .

# 或者如果是本地项目，使用 SCP 上传
# 在本地执行：
# scp -r D:\Project\blog\* root@<服务器 IP>:/www/blog/
```

#### 方法二：通过宝塔面板上传

1. 在本地将项目打包为 zip 文件
2. 登录宝塔面板，进入「文件」
3. 进入 `/www/blog` 目录
4. 点击「上传」按钮，上传 zip 包
5. 右键 zip 文件，选择「解压」

### 4.2 创建 Docker 目录结构

```bash
cd /www/blog

# 创建必要的目录
mkdir -p docker/backend
mkdir -p docker/frontend
mkdir -p docker/nginx/ssl
mkdir -p sql
mkdir -p logs
```

### 4.3 创建后端 Dockerfile

```bash
cat > docker/backend/Dockerfile <<'EOF'
# 多阶段构建 - 构建阶段
# 使用 Maven + Eclipse Temurin JDK 17 (Alpine 版本，更轻量)
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制 pom.xml
COPY blog-backend/pom.xml .

# 下载依赖（使用阿里云 Maven 镜像加速）
RUN mkdir -p ~/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?> \
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
              xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 \
                                  https://maven.apache.org/xsd/settings-1.0.0.xsd"> \
      <mirrors> \
        <mirror> \
          <id>aliyun</id> \
          <name>Aliyun Maven</name> \
          <url>https://maven.aliyun.com/repository/public</url> \
          <mirrorOf>central</mirrorOf> \
        </mirror> \
      </mirrors> \
    </settings>' > ~/.m2/settings.xml

RUN mvn dependency:go-offline -B

# 复制源码
COPY blog-backend/src ./src

# 打包应用
RUN mvn clean package -DskipTests -B

# 运行阶段
# 使用 Eclipse Temurin JRE 17（官方维护，openjdk 镜像已废弃）
FROM eclipse-temurin:17-jre

WORKDIR /app

# 安装时区和 curl（健康检查需要）
RUN apt-get update && apt-get install -y tzdata curl && rm -rf /var/lib/apt/lists/*

# 设置时区
ENV TZ=Asia/Shanghai

# 从构建阶段复制 jar 包
COPY --from=builder /app/target/blog-backend-1.0.0.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads/avatars /app/uploads/images

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=prod"]
EOF
```

### 4.4 创建前端 Dockerfile

```bash
cat > docker/frontend/Dockerfile <<'EOF'
# 多阶段构建 - 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

# 配置 npm 国内镜像
RUN npm config set registry https://registry.npmmirror.com

# 复制 package.json
COPY blog-frontend/package*.json ./

# 安装依赖
RUN npm ci

# 复制源码
COPY blog-frontend/ .

# 构建应用
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# 生产阶段 - 使用 Nginx
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
EOF
```

### 4.5 创建 Nginx 配置

```bash
cat > docker/nginx/nginx.conf <<'EOF'
server {
    listen 80;
    server_name localhost;
    
    root /usr/share/nginx/html;
    index index.html;
    
    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    gzip_min_length 1k;
    
    # 前端静态资源
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # SSE 流式响应支持
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }
    
    # 上传文件访问
    location /uploads/ {
        proxy_pass http://backend:8080/uploads/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
EOF
```

### 4.6 创建 docker-compose.yml

```bash
cat > docker-compose.yml <<'EOF'
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: blog-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-RootPassword123!}
      MYSQL_DATABASE: blog
      MYSQL_USER: blog
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-BlogPassword123!}
      TZ: Asia/Shanghai
      LANG: C.UTF-8
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --init-connect='SET NAMES utf8mb4'
      --skip-character-set-client-handshake
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    networks:
      - blog-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # 后端服务
  backend:
    build:
      context: .
      dockerfile: docker/backend/Dockerfile
    container_name: blog-backend
    restart: always
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      AI_API_KEY: ${AI_API_KEY}
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: blog
      DB_USERNAME: blog
      DB_PASSWORD: ${MYSQL_PASSWORD:-BlogPassword123!}
      JWT_SECRET: ${JWT_SECRET:-YourJwtSecretKey123!@#}
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - uploads-data:/app/uploads
      - ./logs:/app/logs
    networks:
      - blog-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # 前端服务
  frontend:
    build:
      context: .
      dockerfile: docker/frontend/Dockerfile
      args:
        VITE_API_BASE_URL: /api
    container_name: blog-frontend
    restart: always
    depends_on:
      - backend
    ports:
      - "${FRONTEND_PORT:-80}:80"
    networks:
      - blog-network
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 30s
      timeout: 5s
      retries: 3

volumes:
  mysql-data:
    driver: local
  uploads-data:
    driver: local

networks:
  blog-network:
    driver: bridge
EOF
```

### 4.7 创建数据库初始化脚本

```bash
cat > sql/init.sql <<'EOF'
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
EOF
```

**说明：**
- 使用 `INSERT IGNORE` 确保脚本可以重复执行，不会报错
- 所有表使用 `utf8mb4 COLLATE utf8mb4_unicode_ci` 字符集，支持中文和 Emoji
- 数据库由 docker-compose 自动创建，无需手动创建
- 默认账号：admin / admin123

---

## 五、配置环境变量

### 5.1 创建 .env 文件

```bash
cat > .env <<'EOF'
# MySQL 配置
MYSQL_ROOT_PASSWORD=YourStrongRootPassword123!
MYSQL_PASSWORD=YourStrongBlogPassword123!
MYSQL_PORT=3306

# 后端服务配置
BACKEND_PORT=8080
AI_API_KEY=sk-203ad7d1c4c148eb82645d6401810d93
JWT_SECRET=YourVeryLongAndSecureJwtSecretKey123!@#

# 前端服务配置
FRONTEND_PORT=80

# 时区配置
TZ=Asia/Shanghai
EOF
```

### 5.2 修改 .env 文件（重要！）

```bash
# 使用宝塔面板或 vim 编辑
vim .env
```

**必须修改的内容：**
1. `MYSQL_ROOT_PASSWORD` - 设置一个强密码（至少 12 位，包含大小写字母、数字、特殊字符）
2. `MYSQL_PASSWORD` - 设置博客数据库用户密码
3. `JWT_SECRET` - 设置一个随机的长字符串作为 JWT 密钥
4. `AI_API_KEY` - 如果需要 AI 功能，填入您的通义千问 API Key

**修改方法：**
- 在宝塔面板中：文件 → 找到 `.env` 文件 → 点击「编辑」
- 或使用 vim：`vim .env`，按 `i` 进入编辑模式，修改后按 `ESC`，输入 `:wq` 保存退出

---

## 六、构建并启动容器

### 6.1 进入项目目录

```bash
cd /www/blog
```

### 6.2 一键构建并启动（推荐）

```bash
# 使用国内镜像加速构建
export DOCKER_BUILDKIT=1
docker compose up -d --build
```

**国内服务器镜像拉取失败解决方案：**

如果 Docker Hub 无法访问，先手动从镜像源拉取基础镜像：

```bash
# 手动从镜像源拉取基础镜像
docker pull docker.1panel.live/library/maven:3.9-eclipse-temurin-17-alpine
docker pull docker.1panel.live/library/eclipse-temurin:17-jre
docker pull docker.1panel.live/library/node:18-alpine
docker pull docker.1panel.live/library/nginx:alpine

# 重新标记为官方名称
docker tag docker.1panel.live/library/maven:3.9-eclipse-temurin-17-alpine maven:3.9-eclipse-temurin-17-alpine
docker tag docker.1panel.live/library/eclipse-temurin:17-jre eclipse-temurin:17-jre
docker tag docker.1panel.live/library/node:18-alpine node:18-alpine
docker tag docker.1panel.live/library/nginx:alpine nginx:alpine

# 然后执行构建
docker compose up -d --build
```

**预计耗时：**
- 首次构建：10-20 分钟（需下载基础镜像和依赖）
- 后续启动：1-2 分钟

### 6.3 查看构建日志

```bash
# 实时查看日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

### 6.4 查看容器状态

```bash
# 查看所有容器状态
docker compose ps

# 预期输出：
# NAME             STATUS         PORTS
# blog-backend     Up (healthy)   0.0.0.0:8080->8080/tcp
# blog-frontend    Up (healthy)   0.0.0.0:80->80/tcp
# blog-mysql       Up (healthy)   0.0.0.0:3306->3306/tcp
```

### 6.5 停止和重启服务

```bash
# 停止所有服务
docker compose down

# 重启所有服务
docker compose restart

# 重启单个服务
docker compose restart backend
docker compose restart frontend

# 删除所有容器和数据卷（危险操作！慎用）
docker compose down -v
```

---

## 七、数据库初始化

### 7.1 自动初始化

如果您已经创建了 `sql/init.sql` 文件，MySQL 容器会在首次启动时自动执行初始化脚本。

### 7.2 手动初始化（如需要）

```bash
# 进入 MySQL 容器（使用 root 用户）
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!'

# 或者导入 SQL 文件（使用 root 用户，避免权限问题）
docker exec -i blog-mysql mysql -u root -p'YourStrongRootPassword123!' blog < sql/init.sql
```

**注意：**
- 密码为 `.env` 文件中设置的 `MYSQL_ROOT_PASSWORD`
- 建议优先使用 `root` 用户执行初始化，避免 `blog` 用户权限问题

### 7.3 验证数据库

```bash
# 使用 root 用户连接到数据库
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!'

# 执行 SQL 查询
mysql> USE blog;
mysql> SHOW TABLES;
mysql> SELECT * FROM user;
```

**blog 用户登录说明：**
- 用户名：`blog`
- 密码：`.env` 文件中 `MYSQL_PASSWORD` 设置的值（默认：`BlogPassword123!`）
- 如登录失败，请检查 `.env` 配置或重置密码

---

## 八、验证与访问

### 8.1 检查服务状态

```bash
# 检查容器状态
docker compose ps

# 检查端口监听
netstat -tlnp | grep -E '80|8080|3306'
```

### 8.2 测试后端接口

```bash
# 测试健康检查接口
curl http://localhost:8080/api/health

# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### 8.3 访问前端页面

打开浏览器，访问：
```
http://<您的服务器公网 IP>
```

**测试账号：**
- 用户名：`admin`
- 密码：`admin123`

### 8.4 配置防火墙（重要！）

#### 方法一：通过宝塔面板配置

1. 登录宝塔面板
2. 点击左侧菜单「安全」
3. 放行以下端口：
   - 80（HTTP）
   - 8080（后端 API，可选）
   - 3306（MySQL，如需要远程访问）

#### 方法二：通过命令行配置

```bash
# 使用 firewall-cmd（CentOS 7+）
firewall-cmd --zone=public --add-port=80/tcp --permanent
firewall-cmd --zone=public --add-port=8080/tcp --permanent
firewall-cmd --reload

# 或使用 iptables
iptables -I INPUT -p tcp --dport 80 -j ACCEPT
iptables -I INPUT -p tcp --dport 8080 -j ACCEPT
service iptables save
```

### 8.5 配置域名（可选）

如果您有域名，可以在宝塔面板中配置：

1. 进入「网站」→ 「添加站点」
2. 填写域名
3. 选择反向代理
4. 代理地址填写：`http://localhost:80`

---

## 九、日常运维管理

### 9.1 查看日志

```bash
# 查看实时日志
docker compose logs -f

# 查看最近 100 行日志
docker compose logs --tail=100

# 导出日志到文件
docker compose logs > logs.txt
```

### 9.2 进入容器调试

```bash
# 进入后端容器
docker exec -it blog-backend sh

# 进入前端容器
docker exec -it blog-frontend sh

# 进入 MySQL 容器
docker exec -it blog-mysql mysql -u blog -p
```

### 9.3 备份数据库

```bash
# 备份数据库
docker exec blog-mysql mysqldump -u blog -p blog > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
docker exec -i blog-mysql mysql -u blog -p blog < backup_20240101_120000.sql
```

### 9.4 备份上传文件

```bash
# 备份上传文件
docker run --rm \
  -v blog_uploads-data:/data \
  -v /www/blog/backups:/backup \
  alpine tar czf /backup/uploads_$(date +%Y%m%d).tar.gz -C /data .
```

### 9.5 更新项目

```bash
# 进入项目目录
cd /www/blog

# 拉取最新代码（Git 方式）
git pull origin main

# 重新构建并启动
docker compose up -d --build

# 或者只重启特定服务
docker compose restart backend frontend
```

### 9.6 资源监控

```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
df -h

# 查看 Docker 资源占用
docker system df
```

### 9.7 清理无用资源

```bash
# 清理悬空镜像
docker image prune -f

# 清理停止的容器
docker container prune -f

# 清理所有未使用的资源（慎用）
docker system prune -f
```

---

## 十、常见问题排查

### 10.1 容器启动失败

**问题 1：端口被占用**
```bash
# 查看端口占用
netstat -tlnp | grep 80
netstat -tlnp | grep 8080
netstat -tlnp | grep 3306

# 解决方法：修改 .env 文件中的端口配置
# 或停止占用端口的服务
systemctl stop nginx  # 如果 Nginx 占用了 80 端口
```

**问题 2：内存不足**
```bash
# 查看内存使用
free -h

# 解决方法：增加 JVM 参数限制内存
# 修改 docker/backend/Dockerfile，在 ENTRYPOINT 中添加：
# "-Xms256m", "-Xmx512m",
```

**问题 3：构建超时/镜像拉取失败**
```bash
# 症状：failed to solve: maven:xxx: failed to do request: dial tcp: i/o timeout
# 原因：Docker Hub 被墙，无法拉取镜像

# 解决方法1：配置 Docker 镜像加速器
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.1panel.live",
    "https://hub.rat.dev",
    "https://docker.m.daocloud.io"
  ]
}
EOF
systemctl daemon-reload
systemctl restart docker

# 解决方法2：手动从镜像源拉取并重新标记
# 参考上文 "6.2 一键构建并启动" 中的手动拉取方法

# 解决方法3：检查镜像标签是否存在
# 原 openjdk 镜像已废弃，现使用 eclipse-temurin 替代
# 原 maven:3.9-openjdk-17 改为 maven:3.9-eclipse-temurin-17-alpine
```

### 10.2 数据库连接失败

**症状：** 后端日志显示 "Connection refused"

**解决方法：**
```bash
# 1. 检查 MySQL 容器是否正常运行
docker compose ps mysql

# 2. 检查网络连通性
docker exec blog-frontend ping mysql

# 3. 查看 MySQL 日志
docker compose logs mysql

# 4. 等待 MySQL 完全启动（首次启动较慢）
sleep 30
docker compose restart backend
```

### 10.3 数据库用户访问被拒绝

**症状：** `ERROR 1045 (28000): Access denied for user 'blog'@'localhost'`

**原因：**
- 密码输入错误
- `.env` 文件中的密码与实际不一致
- MySQL 用户未正确创建

**解决方法：**
```bash
# 方法1：使用 root 用户登录（推荐）
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!'

# 方法2：查看 .env 文件确认密码
cat /www/blog/.env | grep MYSQL

# 方法3：重置 blog 用户密码
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!' -e "
ALTER USER 'blog'@'%' IDENTIFIED BY '新密码';
FLUSH PRIVILEGES;
"
```

### 10.4 前端无法访问后端

**症状：** 前端页面空白或显示 502 错误

**解决方法：**
```bash
# 1. 检查后端是否启动
docker compose ps backend

# 2. 测试后端接口
curl http://localhost:8080/api/health

# 3. 查看后端日志
docker compose logs backend

# 4. 重启服务
docker compose restart backend frontend
```

### 10.5 AI 助手无响应

**症状：** AI 对话功能无响应或报错

**解决方法：**
1. 检查 `.env` 文件中 `AI_API_KEY` 是否正确配置
2. 验证 API Key 是否有效（登录阿里云百炼平台）
3. 查看后端日志中的 AI 相关错误信息
4. 检查服务器网络连接性

### 10.6 文件上传失败

**症状：** 上传图片或文件时报错

**解决方法：**
```bash
# 1. 检查上传目录权限
docker exec blog-backend ls -la /app/uploads

# 2. 修改目录权限
docker exec blog-backend chmod -R 755 /app/uploads

# 3. 检查 Nginx 配置
docker exec blog-frontend cat /etc/nginx/conf.d/default.conf

# 4. 查看文件大小限制配置
# 确保 application.yml 中配置了合适的 max-file-size
```

### 10.7 数据库字段缺失错误

**症状：** 报错 `Unknown column 'xxx' in 'field list'`

**原因：** 数据库表结构与 Mapper 文件不匹配

**解决方法：**
```bash
# 1. 查看所有表结构
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!' -e "SHOW TABLES;" blog

# 2. 重新执行 init.sql
docker exec -i blog-mysql mysql -u root -p'YourStrongRootPassword123!' blog < sql/init.sql

# 3. 或手动添加缺失字段（以 tag 表为例）
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!' -e "
ALTER TABLE blog.tag ADD COLUMN article_count INT DEFAULT 0;
ALTER TABLE blog.tag ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
" blog
```

### 10.8 中文乱码问题

**症状：** 数据库中的中文显示为乱码

**原因：** 字符集配置不正确

**解决方法：**
```bash
# 1. 检查 MySQL 字符集配置
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!' -e "
SHOW VARIABLES LIKE 'character_set%';
SHOW VARIABLES LIKE 'collation%';
"

# 2. 修改数据库和表的字符集
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!' -e "
ALTER DATABASE blog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE blog;
ALTER TABLE user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE article CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE category CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE tag CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
" blog

# 3. 如果仍有问题，需要重新创建容器（会丢失数据）
docker compose down -v
docker compose up -d
sleep 20
docker exec -i blog-mysql mysql -u root -p'YourStrongRootPassword123!' blog < sql/init.sql
```

### 10.9 获取帮助

```bash
# Docker 帮助
docker --help
docker compose --help

# 查看容器详细信息
docker inspect blog-backend

# 查看网络配置
docker network inspect blog-blog-network
```

---

## 附录 A：快速命令参考

```bash
# === 服务管理 ===
docker compose up -d                    # 启动所有服务
docker compose down                     # 停止所有服务
docker compose restart                  # 重启所有服务
docker compose ps                       # 查看容器状态

# === 日志查看 ===
docker compose logs -f                  # 实时查看所有日志
docker compose logs -f backend          # 查看后端日志
docker compose logs --tail=100          # 查看最近 100 行日志

# === 容器操作 ===
docker exec -it blog-backend sh         # 进入后端容器
docker exec -it blog-frontend sh        # 进入前端容器
docker exec -it blog-mysql mysql -u root -p'YourStrongRootPassword123!'  # 进入 MySQL

# === 数据库操作 ===
docker exec blog-mysql mysqldump -u root -p'YourStrongRootPassword123!' blog > backup.sql  # 备份
docker exec -i blog-mysql mysql -u root -p'YourStrongRootPassword123!' blog < backup.sql   # 恢复

# === 系统清理 ===
docker image prune -f                   # 清理悬空镜像
docker container prune -f               # 清理停止的容器
docker system prune -f                  # 清理所有未使用资源
```

---

## 附录 B：性能优化建议

### B.1 JVM 参数调优

修改 `docker/backend/Dockerfile`:
```dockerfile
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", "app.jar"]
```

### B.2 资源配置

根据服务器配置，在 `docker-compose.yml` 中添加资源限制：
```yaml
services:
  backend:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### B.3 启用 HTTPS（生产环境必须）

1. 在宝塔面板申请 SSL 证书（免费 Let's Encrypt）
2. 配置 Nginx 反向代理 HTTPS
3. 修改 `docker/nginx/nginx.conf` 启用 SSL

---

## 附录 C：安全检查清单

- [ ] 已修改 MySQL 默认密码
- [ ] JWT Secret 已设置为强随机值
- [ ] AI API Key 已正确配置（如需要）
- [ ] 数据库端口 3306 未在公网开放
- [ ] 已配置防火墙规则
- [ ] 定期备份数据库（建议每天）
- [ ] 定期检查容器更新
- [ ] 已配置日志轮转（避免日志占满磁盘）

---

## 联系支持

如遇到无法解决的问题，请提供以下信息：

1. 服务器配置（CPU、内存、磁盘）
2. Docker 版本信息
3. 完整的错误日志
4. 问题复现步骤

**收集诊断信息：**
```bash
# 收集所有相关信息
docker compose ps > diagnosis.txt
docker compose logs >> diagnosis.txt
docker stats --no-stream >> diagnosis.txt
docker inspect blog-backend >> diagnosis.txt
```

将 `diagnosis.txt` 文件内容提供给技术支持人员。

---

**祝您部署成功！** 🎉
