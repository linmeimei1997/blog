# 多阶段构建 - 构建阶段
# 阶段1：使用 Maven 镜像（仅包含 Maven，JDK 单独处理）
FROM maven:3.9.6-slim AS builder

# 安装 Eclipse Temurin JDK 17
RUN apt-get update && \
    apt-get install -y wget apt-transport-https && \
    mkdir -p /etc/apt/keyrings && \
    wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | tee /etc/apt/keyrings/adoptium.asc && \
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-17-jdk && \
    rm -rf /var/lib/apt/lists/*

# 设置 JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

# 复制 pom.xml
COPY blog-backend/pom.xml .

# 下载依赖（使用国内镜像）
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
# 阶段2：仅使用 JRE 运行时（更轻量）
FROM eclipse-temurin:17-jre-slim

WORKDIR /app

# 安装时区
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
