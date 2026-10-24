# Changelog

All notable changes to Duewise are listed here. Versions follow [Semantic Versioning](https://semver.org/):
MAJOR for breaking API changes, MINOR for new features, PATCH for fixes.

## [Unreleased]

## [1.0.0] - 2026-10-25

First release: invoicing, payments and automatic overdue reminders as a REST API.

### Added

- Clients with billing email, address, tax ID, default currency and payment terms; search, sorting and paging.
- Invoices with line items, per-line tax, multiple currencies and per-line rounding to the currency's minor units.
- Draft, send and void workflow; gapless invoice numbers per year (`INV-2026-00001`), assigned on send.
- Full and part payments by bank transfer, UPI, card, cheque or cash; overpayments refused.
- Client statements with balances per currency and ageing buckets (current, 1–30, 31–60, 61–90, 90+ days).
- A4 PDF invoices with DRAFT, PAID and VOID stamps and Indian digit grouping.
- Daily overdue reminders with the PDF attached on configurable days (default 1, 7 and 14), one owner summary per day,
  a log of every attempt, and automatic retry of failed sends.
- Reminder settings API: owner summary recipients (TO and CC), reminder days, on/off switches.
- Currencies and payment methods as reference data that can be added or deactivated through the API.
- Optimistic locking on clients, invoices and reminder settings.
- RFC 9457 problem responses with a stable error `code` and per-field validation errors.
- Swagger UI and OpenAPI 3.1 description.
- Docker image and Docker Compose setup; configuration through environment variables or `.env`.
- 131 automated tests against real PostgreSQL and SMTP servers, a coverage gate and GitHub Actions CI.

[Unreleased]: https://github.com/developeras95/duewise-invoicing-api/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/developeras95/duewise-invoicing-api/releases/tag/v1.0.0
