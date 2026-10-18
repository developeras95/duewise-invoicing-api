package com.duewise.paymentmethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Partial update: only the fields sent are changed. The code is fixed once created. */
public record UpdatePaymentMethodRequest(

        @Schema(example = "PayPal")
        @Size(max = 60)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        String displayName,

        @Schema(example = "true")
        Boolean requiresReference,

        @Schema(description = "Set to false to retire the method; past payments keep it", example = "false")
        Boolean active,

        @Schema(example = "60")
        @Min(0)
        @Max(Short.MAX_VALUE)
        Integer sortOrder) {
}
