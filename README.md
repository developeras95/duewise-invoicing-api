# Duewise — get paid on time

Invoicing, payments and overdue reminders for freelancers and small businesses.

> 🚧 Work in progress. The full README (features, screenshots, example requests and test instructions) is written once the API is complete.

## Quick start

Requirements: Java 21 and Docker Desktop.

```bash
# 1. Start PostgreSQL and Mailpit
docker compose up -d

# 2. Run the application (Flyway creates the schema on startup)
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

| What | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI spec | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |
| Mailpit inbox | http://localhost:8025 |

## Running the tests

```bash
./mvnw test
```

Integration tests start their own PostgreSQL container with Testcontainers, so Docker must be running.

## Tech stack

Java 21 · Spring Boot 4.1 · PostgreSQL 17 · Flyway · Spring Data JPA · springdoc-openapi · JUnit 5 · Mockito · Testcontainers · Docker Compose
