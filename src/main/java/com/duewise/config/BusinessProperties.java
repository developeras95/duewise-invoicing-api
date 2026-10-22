package com.duewise.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The business that issues the invoices: printed on PDFs and emails, set in application.yml. */
@ConfigurationProperties(prefix = "duewise.business")
public record BusinessProperties(
        String timeZone,
        String name,
        List<String> addressLines,
        String email,
        String taxId) {

    public BusinessProperties {
        addressLines = addressLines == null ? List.of() : addressLines.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        email = email == null || email.isBlank() ? null : email.trim();
        taxId = taxId == null || taxId.isBlank() ? null : taxId.trim();
    }
}
