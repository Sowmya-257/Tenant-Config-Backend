# Multi-stage Docker build for Render deployment (Java 21 + Spring Boot 3.3.5)
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Maven POMs and modules
COPY pom.xml ./
COPY config-client-sdk ./config-client-sdk
COPY config-server ./config-server

# Build and package all modules
RUN mvn clean package -DskipTests

# Stage 2: Lightweight JRE 21 runtime container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy executable jar from config-server target
COPY --from=build /app/config-server/target/config-server-*.jar app.jar

# Render assigns a dynamic PORT environment variable (default 8090)
ENV PORT=8090
EXPOSE ${PORT}

# Run Spring Boot service
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
