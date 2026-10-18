package com.duewise.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An ISO 4217 currency. The ISO code is the primary key: it is short, readable and never changes,
 * so invoices can store "INR" directly.
 *
 * <p>{@code minorUnits} (decimal places) cannot be changed after creation, because existing
 * amounts were rounded to it.
 */
@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "code", length = 3, nullable = false, updatable = false)
    private String code;

    @Column(name = "name", length = 60, nullable = false)
    private String name;

    @Column(name = "symbol", length = 5, nullable = false)
    private String symbol;

    @Column(name = "minor_units", nullable = false, updatable = false)
    private short minorUnits;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected Currency() {
        // for JPA
    }

    public Currency(String code, String name, String symbol, short minorUnits) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.minorUnits = minorUnits;
        this.active = true;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public short getMinorUnits() {
        return minorUnits;
    }

    public boolean isActive() {
        return active;
    }
}
