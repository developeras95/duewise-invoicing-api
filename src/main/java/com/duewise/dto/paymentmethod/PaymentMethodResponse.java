package com.duewise.dto.paymentmethod;

import com.duewise.entity.paymentmethod.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

/** The internal numeric id is not exposed; clients use the readable code. */
@Schema(name = "PaymentMethod")
public record PaymentMethodResponse(
        @Schema(example = "UPI") String code,
        @Schema(example = "UPI") String displayName,
        @Schema(description = "Payments with this method must include a transaction or cheque reference")
        boolean requiresReference,
        @Schema(description = "Inactive methods cannot be used for new payments") boolean active,
        @Schema(description = "Display order, lowest first", example = "20") int sortOrder) {

    public static PaymentMethodResponse from(PaymentMethod method) {
        return new PaymentMethodResponse(
                method.getCode(),
                method.getDisplayName(),
                method.isRequiresReference(),
                method.isActive(),
                method.getSortOrder());
    }
}
