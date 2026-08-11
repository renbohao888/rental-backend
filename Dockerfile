# ========== 后端构建镜像（Java 21 + Spring Boot） ==========
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads
ENV FILE_UPLOAD_PATH=/app/uploads \
    SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/room_rent_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8' \
    SPRING_DATASOURCE_USERNAME=root \
    SPRING_DATASOURCE_PASSWORD= \
    SPRING_DATA_REDIS_HOST=localhost \
    SPRING_DATA_REDIS_PORT=6379
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
