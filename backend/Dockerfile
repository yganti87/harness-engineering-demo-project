FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build

# Copy Maven wrapper and pom first (layer caching)
COPY pom.xml .
COPY checkstyle.xml .

# Download dependencies (cached unless pom.xml changes)
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/* && \
    mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# ── Runtime image ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

RUN mkdir -p /var/log/app && chown appuser:appgroup /var/log/app

WORKDIR /app

COPY --from=builder /build/target/library-backend-*.jar app.jar

RUN chown appuser:appgroup /app/app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
