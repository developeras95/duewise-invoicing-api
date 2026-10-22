package com.duewise.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Deployment settings for the reminder job. What changes at runtime (switches, recipients, days)
 * lives in the database and is edited through /api/v1/settings/reminders instead.
 */
@ConfigurationProperties(prefix = "duewise.reminders")
public record ReminderProperties(boolean schedulerEnabled, String cron, List<String> ownerEmails) {

    public ReminderProperties {
        ownerEmails = ownerEmails == null ? List.of() : ownerEmails.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
