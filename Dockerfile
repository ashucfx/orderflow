# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM to leverage layer caching for dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build the artifact
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Ashutosh"
LABEL description="OrderFlow — Enterprise Order & Inventory Backend"

WORKDIR /app

# Add non-root user and group for container security hardening
RUN addgroup -g 1001 -S orderflow && \
    adduser -u 1001 -S orderflow -G orderflow && \
    apk add --no-cache curl

# Copy jar artifact from build stage
COPY --from=builder --chown=orderflow:orderflow /build/target/orderflow-*.jar app.jar

USER orderflow:orderflow

EXPOSE 8080

# Production-tuned JVM configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
