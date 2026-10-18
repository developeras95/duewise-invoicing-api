package com.duewise.paymentmethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A way a client can pay, e.g. bank transfer or UPI. Payments reference this table instead of
 * storing free text, so every payment uses one consistent list. A method that has been used can
 * be deactivated but never deleted (the foreign key is ON DELETE RESTRICT).
 */
@Entity
@Table(name = "payment_method")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Short id;

    @Column(name = "code", length = 30, nullable = false, updatable = false)
    private String code;

    @Column(name = "display_name", length = 60, nullable = false)
    private String displayName;

    @Column(name = "requires_reference", nullable = false)
    private boolean requiresReference;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    protected PaymentMethod() {
        // for JPA
    }

    public PaymentMethod(String code, String displayName, boolean requiresReference, short sortOrder) {
        this.code = code;
        this.displayName = displayName;
        this.requiresReference = requiresReference;
        this.sortOrder = sortOrder;
        this.active = true;
    }

    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public void setRequiresReference(boolean requiresReference) {
        this.requiresReference = requiresReference;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSortOrder(short sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRequiresReference() {
        return requiresReference;
    }

    public boolean isActive() {
        return active;
    }

    public short getSortOrder() {
        return sortOrder;
    }
}
