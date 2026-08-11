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
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
# 数据库等连接配置通过环境变量传入（DB_URL/DB_USERNAME/DB_PASSWORD/REDIS_HOST 等），
# 不要在镜像里写死，避免覆盖云端环境变量。
ENV FILE_UPLOAD_PATH=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
