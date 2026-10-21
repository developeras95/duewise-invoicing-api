package com.duewise.invoice;

import java.time.LocalDate;

/** Optional filters for listing invoices. Null means "don't filter on this". */
record InvoiceFilter(
        InvoiceStatus status,
        Long clientId,
        boolean overdueOnly,
        LocalDate issuedFrom,
        LocalDate issuedTo) {
}
