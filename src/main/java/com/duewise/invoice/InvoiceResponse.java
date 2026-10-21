package com.duewise.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/** Full invoice with its lines. Amounts are shown with the currency's decimal places. */
@Schema(name = "Invoice")
public record InvoiceResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "Null until the invoice is sent", example = "INV-2026-00042") String invoiceNumber,
        InvoiceStatus status,
        @Schema(description = "Money still owed and the due date has passed") boolean overdue,
        ClientRef client,
        @Schema(example = "INR") String currencyCode,
        LocalDate issueDate,
        LocalDate dueDate,
        List<LineItemResponse> lines,
        @Schema(example = "25001.00") BigDecimal subtotal,
        @Schema(example = "4500.18") BigDecimal taxTotal,
        @Schema(example = "29501.18") BigDecimal total,
        @Schema(example = "0.00") BigDecimal amountPaid,
        @Schema(example = "29501.18") BigDecimal balanceDue,
        String notes,
        Instant sentAt,
        Instant paidAt,
        Instant voidedAt,
        String voidReason,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Send this back when updating a draft", example = "0") long version) {

    @Schema(name = "ClientRef")
    public record ClientRef(@Schema(example = "1") Long id, @Schema(example = "Acme Design Studio") String name) {
    }

    @Schema(name = "LineItem")
    public record LineItemResponse(
            @Schema(example = "1") int lineNo,
            String description,
            @Schema(example = "2") BigDecimal quantity,
            @Schema(example = "12500.50") BigDecimal unitPrice,
            @Schema(example = "18") BigDecimal taxRate,
            @Schema(example = "25001.00") BigDecimal lineSubtotal,
            @Schema(example = "4500.18") BigDecimal lineTax,
            @Schema(example = "29501.18") BigDecimal lineTotal) {
    }

    static InvoiceResponse from(Invoice invoice, LocalDate today) {
        int scale = invoice.getCurrency().getMinorUnits();
        List<LineItemResponse> lines = invoice.getLines().stream()
                .map(line -> new LineItemResponse(
                        line.getLineNo(),
                        line.getDescription(),
                        InvoiceMath.trimZeros(line.getQuantity()),
                        InvoiceMath.round(line.getUnitPrice(), scale),
                        InvoiceMath.trimZeros(line.getTaxRate()),
                        InvoiceMath.round(line.getLineSubtotal(), scale),
                        InvoiceMath.round(line.getLineTax(), scale),
                        InvoiceMath.round(line.getLineTotal(), scale)))
                .toList();
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.isOverdue(today),
                new ClientRef(invoice.getClient().getId(), invoice.getClient().getName()),
                invoice.getCurrency().getCode(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                lines,
                InvoiceMath.round(invoice.getSubtotal(), scale),
                InvoiceMath.round(invoice.getTaxTotal(), scale),
                InvoiceMath.round(invoice.getTotal(), scale),
                InvoiceMath.round(invoice.getAmountPaid(), scale),
                InvoiceMath.round(invoice.getBalanceDue(), scale),
                invoice.getNotes(),
                invoice.getSentAt(),
                invoice.getPaidAt(),
                invoice.getVoidedAt(),
                invoice.getVoidReason(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getVersion());
    }
}
