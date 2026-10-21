package com.duewise.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * What a client owes, as of today. Balances are grouped per currency because amounts in different
 * currencies are never added together.
 */
@Schema(name = "Statement")
public record StatementResponse(
        @Schema(example = "1") Long clientId,
        @Schema(example = "Acme Design Studio") String clientName,
        LocalDate asOf,
        List<CurrencyBalance> balances,
        List<OpenInvoice> openInvoices) {

    @Schema(name = "CurrencyBalance", description = "Outstanding money in one currency, split by how late it is")
    public record CurrencyBalance(
            @Schema(example = "INR") String currencyCode,
            @Schema(example = "3") int openInvoices,
            @Schema(example = "5500.00") BigDecimal totalOutstanding,
            @Schema(description = "Not yet due", example = "3000.00") BigDecimal current,
            @Schema(example = "1000.00") BigDecimal days1To30,
            @Schema(example = "1500.00") BigDecimal days31To60,
            @Schema(example = "0.00") BigDecimal days61To90,
            @Schema(example = "0.00") BigDecimal over90) {
    }

    @Schema(name = "OpenInvoice")
    public record OpenInvoice(
            @Schema(example = "7") Long id,
            @Schema(example = "INV-2026-00042") String invoiceNumber,
            @Schema(example = "INR") String currencyCode,
            LocalDate issueDate,
            LocalDate dueDate,
            @Schema(description = "0 when not yet due", example = "12") int daysOverdue,
            AgingBucket agingBucket,
            @Schema(example = "1000.00") BigDecimal balanceDue) {
    }
}
