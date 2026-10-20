package com.duewise.client;

import java.time.Instant;

import com.duewise.currency.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** A customer that invoices are raised for. */
@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @Column(name = "billing_email", length = 254)
    private String billingEmail;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Embedded
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency_code")
    private Currency defaultCurrency;

    @Column(name = "payment_terms_days", nullable = false)
    private short paymentTermsDays;

    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: incremented on every update, so two edits of the same client cannot silently overwrite each other. */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Client() {
        // for JPA
    }

    Client(ClientDetails details) {
        apply(details);
    }

    /** Replaces every editable field (PUT semantics). */
    void apply(ClientDetails details) {
        this.name = details.name();
        this.email = details.email();
        this.billingEmail = details.billingEmail();
        this.phone = details.phone();
        this.taxId = details.taxId();
        this.address = details.address();
        this.defaultCurrency = details.defaultCurrency();
        this.paymentTermsDays = details.paymentTermsDays();
        this.remindersEnabled = details.remindersEnabled();
    }

    /** Where reminders and invoices are emailed: the accounts team if set, otherwise the main contact. */
    public String reminderEmail() {
        return billingEmail != null ? billingEmail : email;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getBillingEmail() {
        return billingEmail;
    }

    public String getPhone() {
        return phone;
    }

    public String getTaxId() {
        return taxId;
    }

    public Address getAddress() {
        return address;
    }

    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public short getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
