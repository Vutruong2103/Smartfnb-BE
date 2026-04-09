# ================================================================
# Stage 1: Build — Maven 3.9 + JDK 21 Alpine
# ================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml trước để cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code và build JAR (bỏ qua tests khi build image)
COPY src ./src
RUN mvn package -DskipTests -q

# ================================================================
# Stage 2: Runtime — image nhỏ gọn với JRE 21
# ================================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="SmartF&B Team <dev@smartfnb.vn>"
LABEL description="SmartF&B SaaS POS — Java 21 + Spring Boot 3.3"

WORKDIR /app

# Tạo user non-root để chạy ứng dụng (bảo mật)
RUN addgroup -S smartfnb && adduser -S smartfnb -G smartfnb

# Copy JAR từ stage build
COPY --from=builder /app/target/*.jar app.jar

# Tạo thư mục uploads và đặt owner trước khi switch sang non-root user
RUN mkdir -p /app/uploads && chown -R smartfnb:smartfnb app.jar /app/uploads

USER smartfnb

# Port ứng dụng
EXPOSE 8080

# Health check — kiểm tra /actuator/health mỗi 30 giây
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Chạy ứng dụng — Virtual Threads kích hoạt qua application.yml (spring.threads.virtual.enabled=true)
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
