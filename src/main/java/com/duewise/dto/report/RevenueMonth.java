package com.duewise.dto.report;

import java.math.BigDecimal;
import java.time.YearMonth;

import io.swagger.v3.oas.annotations.media.Schema;

/** Money received in one month in one currency. */
@Schema(name = "RevenueMonth")
public record RevenueMonth(
        @Schema(type = "string", example = "2026-10") YearMonth month,
        @Schema(example = "INR") String currencyCode,
        @Schema(description = "Number of payments", example = "42") long payments,
        @Schema(description = "Sum of those payments, in the currency", example = "1250000.00") BigDecimal received) {
}
