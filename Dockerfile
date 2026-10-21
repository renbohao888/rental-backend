# ========== 后端构建镜像（Java 21 + Spring Boot） ==========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
# 云端部署使用"模板配置"（无敏感信息，全部通过环境变量注入）；
# 本地真实 application.properties 含密码，不会进入镜像。
RUN cp src/main/resources/application.properties.example src/main/resources/application.properties
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# 安装 curl 供 HEALTHCHECK 使用（基础镜像未内置）
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
# 数据库等连接配置通过环境变量传入（DB_URL/DB_USERNAME/DB_PASSWORD/REDIS_HOST 等），
# 不要在镜像里写死，避免覆盖云端环境变量。
ENV FILE_UPLOAD_PATH=/app/uploads
EXPOSE 8080
# 容器健康探测：命中 /api/health 返回 200 即为健康
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
