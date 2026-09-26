# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — build
# Uses the full JDK + Maven wrapper to compile and package the JAR.
# This stage is never shipped — only the output is copied to stage 2.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy dependency descriptors first so Docker can cache the layer.
# The dependency layer only re-downloads when pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline -q --no-transfer-progress

# Copy source and build, skipping tests (tests run in CI, not in Docker build)
COPY src/ src/
RUN ./mvnw package -DskipTests --no-transfer-progress -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — runtime
# Minimal JRE image — no compiler, no Maven, no source code.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=build /workspace/target/compensation-*.jar app.jar

# Liquibase runs on startup — ensure the app has time to connect to Postgres
# before Spring tries to run migrations.
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
