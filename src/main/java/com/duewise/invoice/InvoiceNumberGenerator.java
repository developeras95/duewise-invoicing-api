package com.duewise.invoice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hands out consecutive invoice numbers per year (INV-2026-00001, INV-2026-00002, ...) with no gaps.
 *
 * <p>One SQL statement creates the year's counter row or increments it, and returns the new value.
 * It runs inside the caller's transaction, so the row stays locked until that transaction ends:
 * two invoices sent at the same moment get different numbers, and if sending fails the increment
 * is rolled back and the number is reused.
 */
@Component
class InvoiceNumberGenerator {

    private static final String NEXT_VALUE_SQL = """
            INSERT INTO invoice_number_counter (invoice_year, last_value)
            VALUES (?, 1)
            ON CONFLICT (invoice_year)
            DO UPDATE SET last_value = invoice_number_counter.last_value + 1
            RETURNING last_value
            """;

    private final JdbcTemplate jdbc;

    InvoiceNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(int year) {
        Integer value = jdbc.queryForObject(NEXT_VALUE_SQL, Integer.class, year);
        return "INV-%d-%05d".formatted(year, value);
    }
}
