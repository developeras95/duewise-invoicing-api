package com.duewise.invoice;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating a draft (POST) and replacing one (PUT). On PUT the lines sent replace all
 * existing lines, and {@code version} is required.
 */
public record InvoiceRequest(

        @Schema(example = "1")
        @NotNull
        Long clientId,

        @Schema(description = "Defaults to the client's default currency", example = "INR")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO 4217 code")
        String currencyCode,

        @Schema(description = "Defaults to today", example = "2026-10-20")
        LocalDate issueDate,

        @Schema(description = "Defaults to issue date + the client's payment terms", example = "2026-11-19")
        LocalDate dueDate,

        @Schema(example = "Thank you for your business.")
        @Size(max = 2000)
        String notes,

        @Size(max = 100)
        List<@NotNull @Valid LineItemRequest> lines,

        @Schema(description = "Required on update: the version you last read. Ignored on create.", example = "0")
        Long version) {
}
