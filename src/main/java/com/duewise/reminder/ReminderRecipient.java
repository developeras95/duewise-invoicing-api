package com.duewise.reminder;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** One person who receives the owner's daily overdue summary. */
@Entity
@Table(name = "reminder_recipient")
public class ReminderRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", length = 254, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", length = 3, nullable = false)
    private DeliveryType deliveryType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ReminderRecipient() {
        // for JPA
    }

    ReminderRecipient(String email, DeliveryType deliveryType) {
        this.email = email;
        this.deliveryType = deliveryType;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }
}
