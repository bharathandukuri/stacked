# ------------------------------------------------------------------------------
# Build Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app

# Copy Maven wrapper and descriptor
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Copy application source code and package executable JAR
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -Dmaven.test.skip=true -B

# ------------------------------------------------------------------------------
# Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runner
WORKDIR /app

# Create non-root system user and storage directories
RUN groupadd -r spring && \
    useradd -r -g spring -u 1001 spring && \
    mkdir -p /app/uploads/temp /app/uploads/permanent && \
    chown -R spring:spring /app

USER spring:spring

COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
