package com.duewise.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update: only the fields sent are changed. The code and decimal places are fixed once a
 * currency exists.
 */
public record UpdateCurrencyRequest(

        @Schema(example = "Swiss Franc")
        @Size(max = 60)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String name,

        @Schema(example = "Fr.")
        @Size(max = 5)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String symbol,

        @Schema(description = "Set to false to stop the currency being used on new invoices", example = "false")
        Boolean active) {
}
