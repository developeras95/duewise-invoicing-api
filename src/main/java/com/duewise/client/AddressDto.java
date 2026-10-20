package com.duewise.client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Postal address as sent and returned by the API. Every field is optional. */
@Schema(name = "Address")
public record AddressDto(
        @Schema(example = "4th Floor, Prestige Tower") @Size(max = 200) String line1,
        @Schema(example = "MG Road") @Size(max = 200) String line2,
        @Schema(example = "Bengaluru") @Size(max = 100) String city,
        @Schema(example = "Karnataka") @Size(max = 100) String state,
        @Schema(example = "560001") @Size(max = 20) String postalCode,
        @Schema(description = "ISO 3166-1 alpha-2 country code", example = "IN")
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be a 2-letter uppercase ISO country code")
        String countryCode) {

    static AddressDto from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(address.getLine1(), address.getLine2(), address.getCity(),
                address.getState(), address.getPostalCode(), address.getCountryCode());
    }
}
