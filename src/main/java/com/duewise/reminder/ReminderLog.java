package com.duewise.reminder;

import java.time.Instant;
import java.time.LocalDate;

import com.duewise.invoice.Invoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * One row per reminder: a client reminder for one invoice at one day offset, or the owner summary
 * for one date. A retry updates the same row, so the partial unique indexes on the table guarantee
 * nothing is ever sent twice.
 */
@Entity
@Table(name = "reminder_log")
public class ReminderLog {

    public enum Type { CLIENT_REMINDER, OWNER_DIGEST }

    public enum Status { SENT, FAILED }

    private static final int MAX_TEXT = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", length = 20, nullable = false, updatable = false)
    private Type reminderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", updatable = false)
    private Invoice invoice;

    @Column(name = "days_overdue", updatable = false)
    private Short daysOverdue;

    @Column(name = "digest_date", updatable = false)
    private LocalDate digestDate;

    @Column(name = "recipients", length = 1000, nullable = false)
    private String recipients;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private Status status;

    @Column(name = "attempts", nullable = false)
    private short attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReminderLog() {
        // for JPA
    }

    static ReminderLog clientReminder(Invoice invoice, short daysOverdue) {
        ReminderLog log = new ReminderLog();
        log.reminderType = Type.CLIENT_REMINDER;
        log.invoice = invoice;
        log.daysOverdue = daysOverdue;
        return log;
    }

    static ReminderLog ownerDigest(LocalDate date) {
        ReminderLog log = new ReminderLog();
        log.reminderType = Type.OWNER_DIGEST;
        log.digestDate = date;
        return log;
    }

    /** Records one attempt: counts it, and keeps the error of a failure for the log endpoint. */
    void recordAttempt(Status outcome, String to, String error, Instant now) {
        this.status = outcome;
        this.recipients = clip(to);
        this.attempts++;
        this.lastError = outcome == Status.FAILED ? clip(error) : null;
        if (outcome == Status.SENT) {
            this.sentAt = now;
        }
        this.updatedAt = now;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    private static String clip(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }

    public Long getId() {
        return id;
    }

    public Type getReminderType() {
        return reminderType;
    }

    public Long getInvoiceId() {
        return invoice == null ? null : invoice.getId();
    }

    public Short getDaysOverdue() {
        return daysOverdue;
    }

    public LocalDate getDigestDate() {
        return digestDate;
    }

    public String getRecipients() {
        return recipients;
    }

    public Status getStatus() {
        return status;
    }

    public short getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
