package com.duewise.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL for the statement: one query over the client's open invoices, with the balance and
 * days overdue worked out by PostgreSQL. This is the query that repository B
 * (duewise-postgres-performance) measures and tunes at millions of rows.
 */
@Repository
class StatementRepository {

    private static final String OPEN_INVOICES_SQL = """
            SELECT i.id,
                   i.invoice_number,
                   i.currency_code,
                   c.minor_units,
                   i.issue_date,
                   i.due_date,
                   i.total - i.amount_paid                             AS balance_due,
                   GREATEST(CAST(:today AS date) - i.due_date, 0)      AS days_overdue
            FROM invoice i
            JOIN currency c ON c.code = i.currency_code
            WHERE i.client_id = :clientId
              AND i.status IN ('SENT', 'PARTIALLY_PAID')
            ORDER BY i.currency_code, i.due_date, i.id
            """;

    record OpenInvoiceRow(Long id, String invoiceNumber, String currencyCode, int minorUnits,
                          LocalDate issueDate, LocalDate dueDate, BigDecimal balanceDue, int daysOverdue) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    StatementRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<OpenInvoiceRow> findOpenInvoices(long clientId, LocalDate today) {
        return jdbc.query(OPEN_INVOICES_SQL, Map.of("clientId", clientId, "today", today), (rs, rowNum) ->
                new OpenInvoiceRow(
                        rs.getLong("id"),
                        rs.getString("invoice_number"),
                        rs.getString("currency_code"),
                        rs.getInt("minor_units"),
                        rs.getObject("issue_date", LocalDate.class),
                        rs.getObject("due_date", LocalDate.class),
                        rs.getBigDecimal("balance_due"),
                        rs.getInt("days_overdue")));
    }
}
