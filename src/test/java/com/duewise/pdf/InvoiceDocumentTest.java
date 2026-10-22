package com.duewise.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.duewise.client.Address;
import org.junit.jupiter.api.Test;

class InvoiceDocumentTest {

    @Test
    void cityAndStateAreSeparatedByACommaAndTheCountryIsSpelledOut() {
        Address address = new Address("4th Floor, Prestige Tower", "MG Road", "Bengaluru", "Karnataka", "560001", "IN");

        assertThat(InvoiceDocument.addressLines(address)).containsExactly(
                "4th Floor, Prestige Tower", "MG Road", "Bengaluru, Karnataka 560001", "India");
    }

    @Test
    void missingPartsLeaveNoStrayCommasOrBlankLines() {
        assertThat(InvoiceDocument.addressLines(new Address("4 Beach Road", null, "Kakinada", null, null, "IN")))
                .containsExactly("4 Beach Road", "Kakinada", "India");
        assertThat(InvoiceDocument.addressLines(new Address("1 Main St", " ", null, "Karnataka", "560001", null)))
                .containsExactly("1 Main St", "Karnataka 560001");
        assertThat(InvoiceDocument.addressLines(null)).isEmpty();
    }

    @Test
    void countryCodesBecomeEnglishNames() {
        assertThat(InvoiceDocument.countryName("IN")).isEqualTo("India");
        assertThat(InvoiceDocument.countryName("us")).isEqualTo("United States");
        assertThat(InvoiceDocument.countryName("GB")).isEqualTo("United Kingdom");
        assertThat(InvoiceDocument.countryName("AE")).isEqualTo("United Arab Emirates");
        assertThat(InvoiceDocument.countryName("ZZ")).as("not an ISO country").isEqualTo("ZZ");
        assertThat(InvoiceDocument.countryName("XX")).isEqualTo("XX");
        assertThat(InvoiceDocument.countryName(null)).isNull();
    }
}
