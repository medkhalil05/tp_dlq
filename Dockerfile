# Multi-stage build: compile in Maven, run in lightweight JDK

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
EXPOSE 8080

# Copy the JAR from the builder stage
COPY --from=builder /app/target/tp-dlq-1.0.0.jar app.jar

# Environment defaults (override with -e at runtime)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    SERVER_PORT=8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 