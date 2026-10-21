package com.duewise.invoice;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LineItemRequest(

        @Schema(example = "Logo design — 3 concepts")
        @NotBlank
        @Size(max = 500)
        String description,

        @Schema(description = "Up to 3 decimal places, e.g. 1.5 hours", example = "2")
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        @Schema(description = "Price per unit. Decimal places may not exceed the currency's (0 for JPY).", example = "12500.50")
        @NotNull
        @DecimalMin("0")
        @Digits(integer = 12, fraction = 3)
        BigDecimal unitPrice,

        @Schema(description = "Tax percentage for this line, e.g. 18 for 18% GST. Defaults to 0.", example = "18")
        @DecimalMin("0")
        @DecimalMax("100")
        @Digits(integer = 3, fraction = 2)
        BigDecimal taxRate) {
}
