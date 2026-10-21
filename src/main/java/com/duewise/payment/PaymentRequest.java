package com.duewise.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentRequest(

        @Schema(description = "In the invoice's currency; at most the balance due", example = "10000.00")
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 12, fraction = 3)
        BigDecimal amount,

        @Schema(description = "Payment method code", example = "UPI")
        @NotBlank
        @Size(max = 30)
        String paymentMethod,

        @Schema(description = "Defaults to today. Not in the future, not before the issue date.", example = "2026-10-21")
        LocalDate paidOn,

        @Schema(description = "Transaction or cheque reference; required for methods that ask for one", example = "UPI-4521987730")
        @Size(max = 100)
        String reference,

        @Schema(example = "Part payment, rest promised next week")
        @Size(max = 500)
        String notes) {
}
