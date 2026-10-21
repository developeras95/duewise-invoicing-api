package com.duewise.invoice;

/**
 * Lifecycle of an invoice.
 *
 * <pre>
 * DRAFT ──send──▶ SENT ──payment──▶ PARTIALLY_PAID ──payment──▶ PAID
 *                   │
 *                   └──void (only while nothing is paid)──▶ VOID
 * </pre>
 *
 * Drafts are deleted rather than voided. PAID and VOID are final.
 */
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PARTIALLY_PAID,
    PAID,
    VOID;

    /** Statuses in which money is still owed, so the invoice can become overdue. */
    public boolean isOutstanding() {
        return this == SENT || this == PARTIALLY_PAID;
    }
}
