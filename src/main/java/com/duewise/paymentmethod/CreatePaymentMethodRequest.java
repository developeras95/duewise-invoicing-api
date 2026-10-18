package com.duewise.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePaymentMethodRequest(

        @Schema(description = "Uppercase letters, digits and underscores, starting with a letter", example = "PAYPAL")
        @NotNull
        @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,29}$",
                message = "must be 1-30 uppercase letters, digits or underscores, starting with a letter")
        String code,

        @Schema(example = "PayPal")
        @NotBlank
        @Size(max = 60)
        String displayName,

        @Schema(description = "Must payments with this method include a reference?", example = "true")
        @NotNull
        Boolean requiresReference,

        @Schema(description = "Display order, lowest first. Defaults to 0.", example = "60")
        @Min(0)
        @Max(Short.MAX_VALUE)
        Integer sortOrder) {
}
