# Stage 1: Build application JAR
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent and module pom.xml files for dependency caching
COPY pom.xml ./
COPY config-client-sdk/pom.xml config-client-sdk/
COPY config-server/pom.xml config-server/

# Copy sources
COPY config-client-sdk/src config-client-sdk/src
COPY config-server/src config-server/src

# Build packages skipping tests inside docker image creation
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S moveinsync && adduser -S moveinsync -G moveinsync
USER moveinsync:moveinsync

# Copy built Spring Boot executable JAR
COPY --from=builder /workspace/config-server/target/config-server-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
