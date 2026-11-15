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

    @Test
    void v6AddsTheMeasuredIndexesAndTheRevenueReport() {
        Integer indexes = jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes WHERE indexname IN (
                    'ix_invoice_open_by_issue_date', 'ix_invoice_client_statement', 'ix_invoice_by_issue_date',
                    'ix_client_name_trgm', 'ix_client_email_trgm', 'ix_payment_paid_on_brin', 'ux_monthly_revenue')
                """, Integer.class);
        Integer reportRefreshes = jdbc.queryForObject(
                "SELECT count(*) FROM report_refresh WHERE report = 'monthly_revenue'", Integer.class);

        assertThat(indexes).isEqualTo(7);
        assertThat(reportRefreshes).isEqualTo(1);
    }
}
