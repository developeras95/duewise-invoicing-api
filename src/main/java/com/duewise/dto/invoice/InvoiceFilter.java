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

    /** True when nothing is filtered: the list is every invoice. */
    public boolean isEmpty() {
        return status == null && clientId == null && !overdueOnly && issuedFrom == null && issuedTo == null;
    }
}
