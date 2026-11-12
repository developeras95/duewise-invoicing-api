package com.duewise.dto.invoice;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** Confirmation that an invoice was emailed: to whom and when. */
@Schema(name = "InvoiceEmail")
public record InvoiceEmailResponse(
        @Schema(example = "42") Long invoiceId,
        @Schema(example = "INV-2026-00042") String invoiceNumber,
        @Schema(description = "The client's billing email, or their main email if there is none",
                example = "accounts@acmedesign.in") String sentTo,
        Instant sentAt) {
}
