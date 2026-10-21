-- Duewise database health check. Read-only: it changes nothing.
-- Run from the project folder:
--   docker compose exec -T postgres psql -U duewise -d duewise < scripts/db-check.sql
-- Every query under "should return 0 rows" must come back empty.

\pset footer off
\echo '== 1. Migrations applied (expect V1 to V5, all success = t)'
SELECT version, description, success, installed_on::date AS installed
FROM flyway_schema_history
ORDER BY installed_rank;

\echo '== 2. Rows per table'
SELECT 'currency' AS table_name, count(*) AS row_count FROM currency
UNION ALL SELECT 'payment_method', count(*) FROM payment_method
UNION ALL SELECT 'client', count(*) FROM client
UNION ALL SELECT 'invoice', count(*) FROM invoice
UNION ALL SELECT 'invoice_line_item', count(*) FROM invoice_line_item
UNION ALL SELECT 'payment', count(*) FROM payment
UNION ALL SELECT 'invoice_number_counter', count(*) FROM invoice_number_counter
UNION ALL SELECT 'reminder_settings', count(*) FROM reminder_settings
UNION ALL SELECT 'reminder_recipient', count(*) FROM reminder_recipient
UNION ALL SELECT 'reminder_offset', count(*) FROM reminder_offset
UNION ALL SELECT 'reminder_log', count(*) FROM reminder_log;

\echo '== 3. Invoices by status and currency'
SELECT status, currency_code, count(*) AS invoices,
       sum(total) AS total, sum(amount_paid) AS paid, sum(total - amount_paid) AS outstanding
FROM invoice
GROUP BY status, currency_code
ORDER BY status, currency_code;

\echo '== 4. Overdue right now (Asia/Kolkata date)'
SELECT count(*) AS overdue_invoices, coalesce(sum(total - amount_paid), 0) AS overdue_amount
FROM invoice
WHERE status IN ('SENT', 'PARTIALLY_PAID')
  AND due_date < (now() AT TIME ZONE 'Asia/Kolkata')::date;

\echo '== 5. amount_paid differs from the sum of payments (should return 0 rows)'
SELECT i.id, i.invoice_number, i.amount_paid, coalesce(sum(p.amount), 0) AS payments_total
FROM invoice i
LEFT JOIN payment p ON p.invoice_id = i.id
GROUP BY i.id, i.invoice_number, i.amount_paid
HAVING i.amount_paid <> coalesce(sum(p.amount), 0);

\echo '== 6. Status does not match the money (should return 0 rows)'
SELECT id, invoice_number, status, total, amount_paid
FROM invoice
WHERE (status = 'PAID'           AND amount_paid <> total)
   OR (status = 'PARTIALLY_PAID' AND (amount_paid = 0 OR amount_paid >= total))
   OR (status IN ('DRAFT', 'SENT', 'VOID') AND amount_paid <> 0)
   OR (status = 'PAID' AND paid_at IS NULL);

\echo '== 7. Line items do not add up to the invoice (should return 0 rows)'
SELECT i.id, i.invoice_number, i.subtotal, sum(l.line_subtotal) AS lines_subtotal,
       i.tax_total, sum(l.line_tax) AS lines_tax
FROM invoice i
JOIN invoice_line_item l ON l.invoice_id = i.id
GROUP BY i.id, i.invoice_number, i.subtotal, i.tax_total
HAVING i.subtotal <> sum(l.line_subtotal) OR i.tax_total <> sum(l.line_tax);

\echo '== 8. Numbering per year: counter, numbers issued, highest number (all three should match)'
SELECT c.invoice_year, c.last_value AS counter,
       count(i.id) AS numbers_issued,
       max(substring(i.invoice_number FROM '\d+$')::int) AS highest_number
FROM invoice_number_counter c
LEFT JOIN invoice i ON i.invoice_number LIKE 'INV-' || c.invoice_year || '-%'
GROUP BY c.invoice_year, c.last_value
ORDER BY c.invoice_year;

\echo '== 9. Gaps in invoice numbers (should return 0 rows)'
WITH numbered AS (
    SELECT substring(invoice_number FROM 5 FOR 4)::int AS yr,
           substring(invoice_number FROM '\d+$')::int AS n
    FROM invoice
    WHERE invoice_number IS NOT NULL
)
SELECT yr AS invoice_year, n + 1 AS missing_number
FROM numbered a
WHERE n < (SELECT max(n) FROM numbered b WHERE b.yr = a.yr)
  AND NOT EXISTS (SELECT 1 FROM numbered b WHERE b.yr = a.yr AND b.n = a.n + 1)
ORDER BY 1, 2;
