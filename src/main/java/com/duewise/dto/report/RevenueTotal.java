package com.duewise.dto.report;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/** Money received over the whole report period in one currency. */
@Schema(name = "RevenueTotal")
public record RevenueTotal(
        @Schema(example = "INR") String currencyCode,
        @Schema(example = "512") long payments,
        @Schema(example = "15430000.00") BigDecimal received) {
}
