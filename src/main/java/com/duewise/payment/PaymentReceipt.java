package com.duewise.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/** What recording a payment returns: the payment, and the invoice's new status and balance. */
@Schema(name = "PaymentReceipt")
public record PaymentReceipt(PaymentResponse payment, InvoiceBalance invoice) {

    @Schema(name = "InvoiceBalance")
    public record InvoiceBalance(
            @Schema(example = "1") Long id,
            @Schema(example = "INV-2026-00042") String invoiceNumber,
            InvoiceStatus status,
            @Schema(example = "INR") String currencyCode,
            @Schema(example = "29501.18") BigDecimal total,
            @Schema(example = "10000.00") BigDecimal amountPaid,
            @Schema(example = "19501.18") BigDecimal balanceDue,
            @Schema(description = "The invoice's new version", example = "3") long version) {
    }

    static PaymentReceipt of(Payment payment, Invoice invoice) {
        int scale = invoice.getCurrency().getMinorUnits();
        InvoiceBalance balance = new InvoiceBalance(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getCurrency().getCode(),
                invoice.getTotal().setScale(scale, RoundingMode.HALF_UP),
                invoice.getAmountPaid().setScale(scale, RoundingMode.HALF_UP),
                invoice.getBalanceDue().setScale(scale, RoundingMode.HALF_UP),
                invoice.getVersion());
        return new PaymentReceipt(PaymentResponse.from(payment, scale), balance);
    }
}
