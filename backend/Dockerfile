# =============================================================================
# Multi-stage Dockerfile for the SecureBank backend.
#
# Stage 1 (build): use a Maven + JDK 21 image to produce the fat jar. We copy the
# POM first and warm the dependency cache so that, on rebuilds where only source
# changed, Docker reuses the cached dependency layer instead of re-downloading.
#
# Stage 2 (runtime): a slim JRE-only image that runs the jar. Keeping the build
# tools out of the final image shrinks it and reduces attack surface.
# =============================================================================

# ---- Stage 1: build ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the application (skip tests in the image build; CI runs them separately).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user for safety.
RUN useradd -r -u 1001 securebank
USER securebank

# The Spring Boot fat jar from the build stage.
COPY --from=build /workspace/target/securebank-api.jar app.jar

EXPOSE 8080

# JVM flags tuned for containers + virtual threads:
#  - MaxRAMPercentage lets the JVM size the heap from the container's memory
#    limit rather than the host's total RAM.
#  - The app uses virtual threads (spring.threads.virtual.enabled) which need no
#    extra JVM flag on Java 21; we just ensure a sane container heap.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Activate the docker profile so service hostnames resolve to compose names.
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
