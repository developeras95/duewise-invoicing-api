package com.duewise.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidInvoiceRequest(
        @Schema(description = "Kept on the invoice for the audit trail", example = "Raised against the wrong client")
        @NotBlank
        @Size(max = 500)
        String reason) {
}
