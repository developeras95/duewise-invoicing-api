package com.duewise.client;

import com.duewise.currency.Currency;

/**
 * The editable fields of a client after the service has cleaned and checked them (trimmed text,
 * resolved currency). Keeps the entity free of request/DTO types.
 */
record ClientDetails(
        String name,
        String email,
        String billingEmail,
        String phone,
        String taxId,
        Address address,
        Currency defaultCurrency,
        short paymentTermsDays,
        boolean remindersEnabled) {
}
