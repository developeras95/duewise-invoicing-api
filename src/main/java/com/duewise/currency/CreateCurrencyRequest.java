package com.duewise.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCurrencyRequest(

        @Schema(description = "ISO 4217 code, uppercase", example = "CHF")
        @NotNull
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO 4217 code")
        String code,

        @Schema(example = "Swiss Franc")
        @NotBlank
        @Size(max = 60)
        String name,

        @Schema(example = "CHF")
        @NotBlank
        @Size(max = 5)
        String symbol,

        @Schema(description = "Decimal places: 2 for most currencies, 0 for JPY, 3 for KWD", example = "2")
        @NotNull
        @Min(0)
        @Max(3)
        Integer minorUnits) {
}
