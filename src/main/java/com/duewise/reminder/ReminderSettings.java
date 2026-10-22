package com.duewise.reminder;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** The single row of reminder switches (the table's CHECK keeps id = 1). */
@Entity
@Table(name = "reminder_settings")
public class ReminderSettings {

    static final short SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "owner_digest_enabled", nullable = false)
    private boolean ownerDigestEnabled;

    @Column(name = "client_reminders_enabled", nullable = false)
    private boolean clientRemindersEnabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ReminderSettings() {
        // for JPA
    }

    void update(boolean ownerDigestEnabled, boolean clientRemindersEnabled, Instant now) {
        this.ownerDigestEnabled = ownerDigestEnabled;
        this.clientRemindersEnabled = clientRemindersEnabled;
        // Always changes, so the version moves even when only recipients or days changed.
        this.updatedAt = now;
    }

    public boolean isOwnerDigestEnabled() {
        return ownerDigestEnabled;
    }

    public boolean isClientRemindersEnabled() {
        return clientRemindersEnabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
