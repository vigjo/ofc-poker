# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage: compile with the full Corretto 25 JDK. Dependencies are
# resolved before the source is copied in so an unrelated code change
# doesn't bust the Maven dependency layer cache.
# ---------------------------------------------------------------------------
FROM amazoncorretto:26-al2023 AS build

WORKDIR /workspace

RUN yum install -y tar gzip >/dev/null && yum clean all && rm -rf /var/cache/yum

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B package -DskipTests -Dquarkus.package.jar.type=fast-jar

# ---------------------------------------------------------------------------
# Runtime stage: headless JRE only, non-root user, just the fast-jar layout
# Quarkus produced (lib/ and app/ are split so unchanged dependency layers
# stay cached across rebuilds).
# ---------------------------------------------------------------------------
FROM amazoncorretto:26-al2023-headless AS runtime

RUN yum install -y shadow-utils >/dev/null && \
    groupadd --system --gid 1000 quarkus && \
    useradd --system --uid 1000 --gid quarkus --home-dir /app --shell /sbin/nologin quarkus && \
    yum remove -y shadow-utils >/dev/null && \
    yum clean all && rm -rf /var/cache/yum

WORKDIR /app

COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/*.jar ./
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/app/ ./app/
COPY --from=build --chown=quarkus:quarkus /workspace/target/quarkus-app/quarkus/ ./quarkus/
COPY --chown=quarkus:quarkus docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh && mkdir -p secrets && chown quarkus:quarkus secrets

USER quarkus:quarkus

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD ["curl", "-f", "http://localhost:8080/health/ready"]

ENTRYPOINT ["./docker-entrypoint.sh"]
