package com.duewise;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that run the whole application against a real PostgreSQL in Docker.
 *
 * <p>The container is started once and shared by every integration test class (the "singleton
 * container" pattern), so the database boots once per test run instead of once per class.
 * {@link ServiceConnection} points the application's datasource at it automatically.
 */
@SpringBootTest(properties = "duewise.reminders.scheduler-enabled=false")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
