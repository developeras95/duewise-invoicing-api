package com.duewise.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating a client (POST) and for replacing one (PUT). On PUT every field is replaced,
 * so omitted optional fields are cleared, and {@code version} is required.
 */
public record ClientRequest(

        @Schema(example = "Acme Design Studio")
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Main contact email. Unique across clients, ignoring case.", example = "asha@acmedesign.in")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Schema(description = "Accounts team email for invoices and reminders, if different", example = "accounts@acmedesign.in")
        @Email
        @Size(max = 254)
        String billingEmail,

        @Schema(example = "+91 80 4567 8900")
        @Pattern(regexp = "^\\+?[0-9 ()-]{5,30}$", message = "must be a phone number of digits, spaces, (), - and an optional leading +")
        String phone,

        @Schema(description = "Tax registration number, e.g. GSTIN or VAT", example = "29ABCDE1234F1Z5")
        @Size(max = 50)
        String taxId,

        @Valid
        AddressDto address,

        @Schema(description = "Currency pre-filled on new invoices for this client", example = "INR")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "must be a 3-letter ISO 4217 code")
        String defaultCurrencyCode,

        @Schema(description = "Days from issue date to due date. Defaults to 30.", example = "30")
        @Min(0)
        @Max(365)
        Integer paymentTermsDays,

        @Schema(description = "Send overdue reminders to this client. Defaults to true.", example = "true")
        Boolean remindersEnabled,

        @Schema(description = "Required on update: the version you last read. Ignored on create.", example = "0")
        Long version) {
}
