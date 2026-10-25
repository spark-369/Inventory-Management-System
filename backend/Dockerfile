# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage — produces the Spring Boot fat jar using the project's Maven
# wrapper, so the build needs no Maven installation on the host or in the image.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

# Copy the wrapper and the POM first. This layer only changes when pom.xml
# changes, so dependencies are not re-resolved on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Now the sources — a code change no longer invalidates the dependency cache.
COPY src/ src/

# Spring Boot also emits a *-plain.jar (classes only), which is not runnable.
# The find expression selects the single executable jar.
RUN ./mvnw -B -ntp -DskipTests clean package \
 && find target -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

# ---------------------------------------------------------------------------
# Runtime stage — JRE only, running as an unprivileged user.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime

RUN groupadd --system --gid 1001 ims \
 && useradd --system --uid 1001 --gid ims --create-home ims

WORKDIR /app
COPY --from=build --chown=ims:ims /workspace/app.jar app.jar

USER ims

# 8080 = REST API, 8000 = JobRunr dashboard.
EXPOSE 8080 8000

# MaxRAMPercentage makes the JVM respect the container memory limit instead of
# sizing the heap from the host's total RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# Configuration is supplied through environment variables, which Spring Boot
# binds with relaxed rules, e.g.:
#   SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
#   REDIS_HOST, REDIS_PORT
#   RATE_LIMIT_ENABLED, RATE_LIMIT_REQUESTS, RATE_LIMIT_WINDOW_SECONDS
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
