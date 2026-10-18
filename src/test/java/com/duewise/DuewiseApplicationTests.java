package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Checks that the application starts and every Flyway migration ran with its seed data. */
class DuewiseApplicationTests extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationsCreateSchemaAndSeedReferenceData() {
        Integer currencies = jdbc.queryForObject("SELECT count(*) FROM currency", Integer.class);
        Integer paymentMethods = jdbc.queryForObject("SELECT count(*) FROM payment_method", Integer.class);
        Integer reminderOffsets = jdbc.queryForObject("SELECT count(*) FROM reminder_offset", Integer.class);
        Integer yenDecimals = jdbc.queryForObject(
                "SELECT minor_units FROM currency WHERE code = 'JPY'", Integer.class);

        assertThat(currencies).isGreaterThanOrEqualTo(8);
        assertThat(paymentMethods).isGreaterThanOrEqualTo(5);
        assertThat(reminderOffsets).isEqualTo(3);
        assertThat(yenDecimals).isZero();
    }
}
