-- Indexes measured at 5 million invoices in the performance study
-- (https://github.com/developeras95/duewise-postgres-performance), and the monthly revenue report.
-- Each index names the case that measured it; the study has the plans, timings and costs.
--
-- On a large existing database these statements lock the tables while they build (the study's
-- 5 million invoices take a few seconds in total). To build them without blocking writes, create
-- them by hand with CREATE INDEX CONCURRENTLY under the same names first; IF NOT EXISTS then skips them.

-- Case 1, the overdue invoices list (339 ms to 0.56 ms): open invoices only, in the list's order,
-- with the due date in the index so "overdue" is checked without reading the table.
CREATE INDEX IF NOT EXISTS ix_invoice_open_by_issue_date
    ON invoice (issue_date DESC, id DESC, due_date)
    WHERE status IN ('SENT', 'PARTIALLY_PAID');

-- Case 2, the client statement (95 ms to 7.1 ms for the largest client): one client's open invoices
-- in the statement's order, carrying every column it prints, so the table is never read.
CREATE INDEX IF NOT EXISTS ix_invoice_client_statement
    ON invoice (client_id, currency_code, due_date, id)
    INCLUDE (invoice_number, issue_date, total, amount_paid)
    WHERE status IN ('SENT', 'PARTIALLY_PAID');

-- Case 4, the invoice list as it first opens (959 ms to 0.42 ms): every invoice, newest first.
CREATE INDEX IF NOT EXISTS ix_invoice_by_issue_date
    ON invoice (issue_date DESC, id DESC);

-- Case 3, client search (18-20 ms to under 2 ms): trigram indexes, so LIKE '%text%' on the lowercased
-- name or email is answered from an index. An index on (name, id) was measured and left out: it made
-- some searches slower than none.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS ix_client_name_trgm ON client USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS ix_client_email_trgm ON client USING gin (lower(email) gin_trgm_ops);

-- Case 5, the monthly revenue report.
-- A BRIN index on the payment date: a few kilobytes, because payments are stored in about the order
-- they are received. It finds the current month's payments, which the report adds up live.
CREATE INDEX IF NOT EXISTS ix_payment_paid_on_brin ON payment USING brin (paid_on);

-- Money received per month and currency, computed once and stored (one row per month and currency).
-- The report reads finished months from here; it is refreshed nightly and on request, see
-- RevenueReportService. The unique index allows REFRESH ... CONCURRENTLY, which keeps it readable
-- while it is rebuilt.
CREATE MATERIALIZED VIEW IF NOT EXISTS monthly_revenue AS
SELECT CAST(date_trunc('month', p.paid_on) AS date) AS month,
       i.currency_code,
       count(*)                                     AS payments,
       sum(p.amount)                                AS received
FROM payment p
JOIN invoice i ON i.id = p.invoice_id
GROUP BY 1, 2;

CREATE UNIQUE INDEX IF NOT EXISTS ux_monthly_revenue ON monthly_revenue (month, currency_code);

-- When each stored report was last brought up to date, shown with the report.
CREATE TABLE report_refresh (
    report       VARCHAR(50) PRIMARY KEY,
    refreshed_at TIMESTAMPTZ NOT NULL
);

INSERT INTO report_refresh (report, refreshed_at) VALUES ('monthly_revenue', now());
