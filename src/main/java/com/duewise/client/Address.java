package com.duewise.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A postal address stored as columns on the client table. Being an embeddable value object means it
 * has no id of its own and is always saved and loaded together with its client.
 */
@Embeddable
public class Address {

    @Column(name = "address_line1", length = 200)
    private String line1;

    @Column(name = "address_line2", length = 200)
    private String line2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "country_code", length = 2)
    private String countryCode;

    protected Address() {
        // for JPA
    }

    public Address(String line1, String line2, String city, String state, String postalCode, String countryCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
