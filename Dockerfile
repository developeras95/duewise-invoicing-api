# syntax=docker/dockerfile:1
#
# Duewise Invoicing API as a container image. Needs only Docker: Java and Maven run inside the build.
#   docker build -t duewise-invoicing-api .
#   docker compose --profile app up --build     (app + Postgres + Mailpit)
#
# Tests are not run here: the integration tests start their own PostgreSQL with Testcontainers,
# which needs Docker inside the build. CI (./mvnw verify) runs every test before code reaches main.

# ---- 1. Build the jar -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# Maven wrapper and pom first: this layer (and the downloaded dependencies) is reused until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw --batch-mode --no-transfer-progress -DskipTests package

# Split the fat jar into layers that change at different speeds (libraries rarely, our code often),
# so a code change only rebuilds and re-downloads the small application layer.
RUN cp target/duewise-invoicing-api-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted \
 && mkdir -p extracted/dependencies extracted/spring-boot-loader extracted/snapshot-dependencies extracted/application

# ---- 2. Runtime image -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble

LABEL org.opencontainers.image.title="Duewise Invoicing API" \
      org.opencontainers.image.description="Invoicing, payments and overdue reminders for freelancers and small businesses" \
      org.opencontainers.image.source="https://github.com/developeras95/duewise-invoicing-api" \
      org.opencontainers.image.licenses="LicenseRef-Proprietary"

# Run as an unprivileged user, never as root.
RUN groupadd --system duewise && useradd --system --gid duewise --no-create-home duewise

WORKDIR /application
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
# Licence and third-party notices travel with the image (the LGPL notice must ship with the library).
COPY LICENSE THIRD-PARTY-NOTICES.md THIRD-PARTY.txt ./

USER duewise
EXPOSE 8080

# Size the heap from the container's memory limit instead of the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["java", "-jar", "application.jar"]
