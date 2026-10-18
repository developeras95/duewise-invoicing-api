package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts the whole application against a real PostgreSQL in Docker and checks
 * that every Flyway migration ran and the seed data is in place.
 */
@Testcontainers
@SpringBootTest
class DuewiseApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsCreateSchemaAndSeedReferenceData() {
        Integer currencies = jdbc.queryForObject("SELECT count(*) FROM currency", Integer.class);
        Integer paymentMethods = jdbc.queryForObject("SELECT count(*) FROM payment_method", Integer.class);
        Integer reminderOffsets = jdbc.queryForObject("SELECT count(*) FROM reminder_offset", Integer.class);
        Integer yenDecimals = jdbc.queryForObject(
                "SELECT minor_units FROM currency WHERE code = 'JPY'", Integer.class);

        assertThat(currencies).isEqualTo(8);
        assertThat(paymentMethods).isEqualTo(5);
        assertThat(reminderOffsets).isEqualTo(3);
        assertThat(yenDecimals).isZero();
    }
}
