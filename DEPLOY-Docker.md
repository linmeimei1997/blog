# 博客系统 Docker 部署文档

## 目录结构

```
blog/
├── docker/
│   ├── backend/
│   │   └── Dockerfile          # 后端镜像构建文件
│   ├── frontend/
│   │   └── Dockerfile          # 前端镜像构建文件
│   └── nginx/
│       ├── nginx.conf          # Nginx 配置
│       └── ssl/                # SSL 证书目录（可选）
├── blog-backend/               # 后端源码
├── blog-frontend/              # 前端源码
├── uploads/                    # 上传文件目录
└── docker-compose.yml          # Docker Compose 配置
```

---

## 一、环境要求

### 1.1 软件版本
| 软件 | 版本要求 | 说明 |
|------|---------|------|
| Docker | 20.10+ | 容器运行环境 |
| Docker Compose | 2.0+ | 容器编排工具 |
| JDK | 17+ | 仅本地构建需要 |
| Node.js | 18+ | 仅本地构建需要 |

### 1.2 安装 Docker

**Ubuntu/Debian:**
```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash -s docker

# 启动 Docker
systemctl start docker
systemctl enable docker

# 验证安装
docker --version
docker compose version
```

**CentOS/RHEL:**
```bash
# 安装依赖
yum install -y yum-utils device-mapper-persistent-data lvm2

# 添加 Docker 仓库
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 安装 Docker
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动 Docker
systemctl start docker
systemctl enable docker
```

---

## 二、Dockerfile 配置

### 2.1 后端 Dockerfile

创建 `docker/backend/Dockerfile`:

```dockerfile
# 多阶段构建 - 构建阶段
FROM maven:3.9-openjdk-17 AS builder

WORKDIR /app

# 复制 pom.xml
COPY blog-backend/pom.xml .

# 下载依赖
RUN mvn dependency:go-offline -B

# 复制源码
COPY blog-backend/src ./src

# 打包应用
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM openjdk:17-slim

WORKDIR /app

# 安装时区
RUN apt-get update && apt-get install -y tzdata && rm -rf /var/lib/apt/lists/*

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
```

### 2.2 前端 Dockerfile

创建 `docker/frontend/Dockerfile`:

```dockerfile
# 多阶段构建 - 构建阶段
FROM node:18-alpine AS builder

WORKDIR /app

# 复制 package.json
COPY blog-frontend/package*.json ./

# 安装依赖
RUN npm ci --registry=https://registry.npmmirror.com

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
```

### 2.3 Nginx 配置

创建 `docker/nginx/nginx.conf`:

```nginx
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
```

---

## 三、Docker Compose 配置

在项目根目录创建 `docker-compose.yml`:

```yaml
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
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d  # 初始化 SQL 脚本
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
```

---

## 四、环境变量配置

在项目根目录创建 `.env` 文件:

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=YourStrongRootPassword123!
MYSQL_PASSWORD=YourStrongBlogPassword123!
MYSQL_PORT=3306

# 后端服务配置
BACKEND_PORT=8080
AI_API_KEY=sk-your-dashscope-api-key
JWT_SECRET=YourVeryLongAndSecureJwtSecretKey123!@#

# 前端服务配置
FRONTEND_PORT=80

# 时区配置
TZ=Asia/Shanghai
```

---

## 五、快速部署

### 5.1 一键部署

```bash
# 克隆项目（如果还没有）
git clone <your-repository-url>
cd blog

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，填入实际的密码和 API Key

# 构建并启动所有服务
docker compose up -d --build

# 查看日志
docker compose logs -f

# 停止服务
docker compose down

# 重启服务
docker compose restart
```

### 5.2 单独构建

```bash
# 只构建后端
docker compose build backend

# 只构建前端
docker compose build frontend

# 只启动后端
docker compose up -d backend

# 只启动前端
docker compose up -d frontend
```

---

## 六、数据库初始化

### 6.1 自动初始化

将 SQL 脚本放在 `sql/` 目录下，Docker 会自动执行：

```
sql/
└── init.sql  # 数据库初始化脚本
```

### 6.2 手动初始化

```bash
# 进入 MySQL 容器
docker exec -it blog-mysql mysql -u root -p

# 或者导入 SQL 文件
docker exec -i blog-mysql mysql -u blog -p blog < sql/init.sql
```

---

## 七、常用命令

### 7.1 容器管理

```bash
# 查看所有容器状态
docker compose ps

# 查看容器日志
docker compose logs backend
docker compose logs frontend
docker compose logs mysql

# 实时查看日志
docker compose logs -f backend

# 重启单个服务
docker compose restart backend

# 停止所有服务
docker compose down

# 删除所有容器和数据卷（危险操作！）
docker compose down -v
```

### 7.2 进入容器

```bash
# 进入后端容器
docker exec -it blog-backend sh

# 进入前端容器
docker exec -it blog-frontend sh

# 进入 MySQL 容器
docker exec -it blog-mysql mysql -u blog -p
```

### 7.3 数据备份

```bash
# 备份 MySQL 数据库
docker exec blog-mysql mysqldump -u blog -p blog > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i blog-mysql mysql -u blog -p blog < backup_20240101.sql

# 备份上传文件
docker run --rm \
  -v blog_uploads-data:/data \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/uploads_$(date +%Y%m%d).tar.gz -C /data .
```

---

## 八、HTTPS 配置（可选）

### 8.1 使用 Certbot 申请证书

```bash
# 在宿主机上安装 certbot
apt-get install certbot python3-certbot-nginx

# 申请证书
certbot --nginx -d your-domain.com

# 证书会保存在 /etc/letsencrypt/live/your-domain.com/
```

### 8.2 配置 HTTPS

修改 `docker/nginx/nginx.conf`:

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # ... 其他配置
    
    # HTTP 跳转
    location / {
        if ($scheme = http) {
            return 301 https://$server_name$request_uri;
        }
    }
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

挂载证书目录:

```yaml
# docker-compose.yml
services:
  frontend:
    volumes:
      - /etc/letsencrypt:/etc/nginx/ssl:ro
```

---

## 九、性能优化

### 9.1 JVM 参数调优

修改 `docker/backend/Dockerfile`:

```dockerfile
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", "app.jar"]
```

### 9.2 Nginx 优化

在 `nginx.conf` 中添加:

```nginx
# worker 进程数
worker_processes auto;

# 连接数限制
events {
    worker_connections 1024;
}

# 开启缓存
http {
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m max_size=1g inactive=60m use_temp_path=off;
}
```

---

## 十、监控与日志

### 10.1 资源监控

```bash
# 查看容器资源使用
docker stats

# 查看容器详细信息
docker inspect blog-backend
```

### 10.2 日志轮转

创建 `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  frontend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  mysql:
    logging:
      driver: "json-file"
      options:
        max-size: "20m"
        max-file: "5"
```

---

## 十一、故障排查

### 11.1 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 容器启动失败 | 端口被占用 | 修改 .env 中的端口配置 |
| 数据库连接失败 | 网络不通 | 检查是否在同一个 network |
| 502 Bad Gateway | 后端未启动 | `docker compose ps` 查看状态 |
| 图片无法访问 | 权限问题 | `chmod -R 755` 上传目录 |
| AI 助手无响应 | API Key 错误 | 检查 .env 配置 |

### 11.2 调试命令

```bash
# 查看容器网络
docker network inspect blog-blog-network

# 测试容器间网络连通性
docker exec blog-frontend ping backend

# 查看容器环境变量
docker exec blog-backend env

# 测试后端接口
curl http://localhost:8080/api/health
```

---

## 十二、CI/CD集成

### 12.1 GitHub Actions 示例

创建 `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Production

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and push backend
        uses: docker/build-push-action@v4
        with:
          context: .
          file: docker/backend/Dockerfile
          push: true
          tags: your-username/blog-backend:latest
      
      - name: Build and push frontend
        uses: docker/build-push-action@v4
        with:
          context: .
          file: docker/frontend/Dockerfile
          push: true
          tags: your-username/blog-frontend:latest
      
      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /path/to/blog
            docker compose pull
            docker compose up -d
```

---

## 十三、安全检查清单

- [ ] 已修改默认密码
- [ ] JWT Secret 已设置为强随机值
- [ ] AI API Key 已正确配置
- [ ] 数据库端口未暴露在公网
- [ ] 已配置防火墙规则
- [ ] SSL 证书已配置（生产环境）
- [ ] 日志轮转已配置
- [ ] 定期备份已配置
- [ ] 容器资源限制已配置

---

## 十四、总结

### 优势
✅ 一键部署，简化运维  
✅ 环境隔离，避免依赖冲突  
✅ 易于扩展和迁移  
✅ 资源利用率高  
✅ 便于 CI/CD 集成  

### 注意事项
⚠️ 数据持久化要配置 volume  
⚠️ 敏感信息使用环境变量  
⚠️ 生产环境务必配置 HTTPS  
⚠️ 定期检查容器安全更新  
⚠️ 配置合理的资源限制  

---

## 联系方式

如有问题，请查看容器日志或联系运维人员。

```bash
# 快速获取帮助
docker compose --help
docker --help
```
