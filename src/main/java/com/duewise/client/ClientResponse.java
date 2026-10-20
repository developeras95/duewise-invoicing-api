package com.duewise.client;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Client")
public record ClientResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Acme Design Studio") String name,
        @Schema(example = "asha@acmedesign.in") String email,
        @Schema(example = "accounts@acmedesign.in") String billingEmail,
        @Schema(example = "+91 80 4567 8900") String phone,
        @Schema(example = "29ABCDE1234F1Z5") String taxId,
        AddressDto address,
        @Schema(example = "INR") String defaultCurrencyCode,
        @Schema(example = "30") int paymentTermsDays,
        boolean remindersEnabled,
        Instant createdAt,
        Instant updatedAt,
        @Schema(description = "Send this back when updating the client", example = "0") long version) {

    static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getBillingEmail(),
                client.getPhone(),
                client.getTaxId(),
                AddressDto.from(client.getAddress()),
                client.getDefaultCurrency() == null ? null : client.getDefaultCurrency().getCode(),
                client.getPaymentTermsDays(),
                client.isRemindersEnabled(),
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getVersion());
    }
}
