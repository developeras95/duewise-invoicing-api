package com.duewise.repository.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import com.duewise.dto.report.RevenueMonth;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL for the revenue report. Finished months are read from the materialized view monthly_revenue
 * (one row per month and currency, created in V6); the current month is added up from the payments,
 * which the BRIN index on payment.paid_on finds without reading the whole table. Measured in case 5 of
 * the performance study (https://github.com/developeras95/duewise-postgres-performance).
 */
@Repository
public class RevenueReportRepository {

    private static final String STORED_MONTHS_SQL = """
            SELECT month, currency_code, payments, received
            FROM monthly_revenue
            WHERE month >= :from AND month < :until
            ORDER BY month, currency_code
            """;

    private static final String LIVE_MONTH_SQL = """
            SELECT i.currency_code, count(*) AS payments, sum(p.amount) AS received
            FROM payment p
            JOIN invoice i ON i.id = p.invoice_id
            WHERE p.paid_on >= :from AND p.paid_on < :until
            GROUP BY i.currency_code
            ORDER BY i.currency_code
            """;

    private static final String REFRESHED_AT_SQL =
            "SELECT refreshed_at FROM report_refresh WHERE report = 'monthly_revenue'";

    private final NamedParameterJdbcTemplate jdbc;

    public RevenueReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The stored months from {@code from} up to and including {@code to}. */
    public List<RevenueMonth> storedMonths(YearMonth from, YearMonth to) {
        Map<String, Object> period = Map.of("from", from.atDay(1), "until", to.plusMonths(1).atDay(1));
        return jdbc.query(STORED_MONTHS_SQL, period, (rs, rowNum) -> new RevenueMonth(
                YearMonth.from(rs.getObject("month", LocalDate.class)),
                rs.getString("currency_code"),
                rs.getLong("payments"),
                rs.getBigDecimal("received")));
    }

    /** One month added up from the payments themselves, so it includes every payment recorded so far. */
    public List<RevenueMonth> liveMonth(YearMonth month) {
        Map<String, Object> period = Map.of("from", month.atDay(1), "until", month.plusMonths(1).atDay(1));
        return jdbc.query(LIVE_MONTH_SQL, period, (rs, rowNum) -> new RevenueMonth(
                month,
                rs.getString("currency_code"),
                rs.getLong("payments"),
                nonNull(rs.getBigDecimal("received"))));
    }

    public Instant refreshedAt() {
        return jdbc.getJdbcTemplate().queryForObject(REFRESHED_AT_SQL, OffsetDateTime.class).toInstant();
    }

    /**
     * Recomputes every month from the payments. CONCURRENTLY keeps the old figures readable meanwhile; the
     * whole recomputation takes about 1.3 s at 4.6 million payments.
     */
    public Instant refresh() {
        jdbc.getJdbcTemplate().execute("REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_revenue");
        return jdbc.getJdbcTemplate().queryForObject(
                "UPDATE report_refresh SET refreshed_at = now() WHERE report = 'monthly_revenue' RETURNING refreshed_at",
                OffsetDateTime.class).toInstant();
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
