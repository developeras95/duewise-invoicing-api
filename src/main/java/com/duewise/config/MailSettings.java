package com.duewise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sender details for every email Duewise sends. The SMTP server itself is configured under spring.mail. */
@ConfigurationProperties(prefix = "duewise.mail")
public record MailSettings(String from, String replyTo) {

    public MailSettings {
        replyTo = replyTo == null || replyTo.isBlank() ? null : replyTo.trim();
    }
}
