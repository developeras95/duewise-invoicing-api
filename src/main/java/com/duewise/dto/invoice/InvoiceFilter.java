package com.duewise.dto.invoice;

import java.time.LocalDate;

import com.duewise.entity.invoice.InvoiceStatus;

/** Optional filters for listing invoices. Null means "don't filter on this". */
public record InvoiceFilter(
        InvoiceStatus status,
        Long clientId,
        boolean overdueOnly,
        LocalDate issuedFrom,
        LocalDate issuedTo) {
}
