package com.duewise.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Payment")
public record PaymentResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "UPI") String paymentMethod,
        @Schema(example = "UPI") String paymentMethodName,
        @Schema(example = "10000.00") BigDecimal amount,
        LocalDate paidOn,
        @Schema(example = "UPI-4521987730") String reference,
        String notes,
        Instant createdAt) {

    static PaymentResponse from(Payment payment, int minorUnits) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentMethod().getCode(),
                payment.getPaymentMethod().getDisplayName(),
                payment.getAmount().setScale(minorUnits, RoundingMode.HALF_UP),
                payment.getPaidOn(),
                payment.getReference(),
                payment.getNotes(),
                payment.getCreatedAt());
    }
}
