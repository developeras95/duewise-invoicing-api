package com.duewise.dto.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.duewise.entity.invoice.Invoice;
import com.duewise.entity.invoice.InvoiceStatus;
import com.duewise.util.invoice.InvoiceMath;
import io.swagger.v3.oas.annotations.media.Schema;

/** One row in the invoice list: enough to show a table without loading line items. */
@Schema(name = "InvoiceSummary")
public record InvoiceSummaryResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "INV-2026-00042") String invoiceNumber,
        InvoiceStatus status,
        boolean overdue,
        @Schema(example = "1") Long clientId,
        @Schema(example = "Acme Design Studio") String clientName,
        @Schema(example = "INR") String currencyCode,
        LocalDate issueDate,
        LocalDate dueDate,
        @Schema(example = "29501.18") BigDecimal total,
        @Schema(example = "29501.18") BigDecimal balanceDue) {

    public static InvoiceSummaryResponse from(Invoice invoice, LocalDate today) {
        int scale = invoice.getCurrency().getMinorUnits();
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.isOverdue(today),
                invoice.getClient().getId(),
                invoice.getClient().getName(),
                invoice.getCurrency().getCode(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                InvoiceMath.round(invoice.getTotal(), scale),
                InvoiceMath.round(invoice.getBalanceDue(), scale));
    }
}
