package com.duewise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the invoice list counts. Counting every invoice for an unfiltered list reads every row (200 ms at
 * 5 million invoices, growing with the table), so above {@code estimateTotalAbove} invoices the
 * unfiltered list takes PostgreSQL's own estimate instead and says so with totalExact=false. Filtered
 * lists are always counted exactly.
 */
@ConfigurationProperties(prefix = "duewise.invoices")
public record InvoiceListProperties(long estimateTotalAbove) {
}
