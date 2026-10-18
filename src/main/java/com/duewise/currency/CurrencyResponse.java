package com.duewise.currency;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Currency")
public record CurrencyResponse(
        @Schema(example = "INR") String code,
        @Schema(example = "Indian Rupee") String name,
        @Schema(example = "₹") String symbol,
        @Schema(description = "Decimal places used for amounts", example = "2") int minorUnits,
        @Schema(description = "Inactive currencies cannot be used on new invoices") boolean active) {

    static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(
                currency.getCode(),
                currency.getName(),
                currency.getSymbol(),
                currency.getMinorUnits(),
                currency.isActive());
    }
}
