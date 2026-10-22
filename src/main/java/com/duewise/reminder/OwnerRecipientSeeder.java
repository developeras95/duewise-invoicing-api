package com.duewise.reminder;

import com.duewise.config.ReminderProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On first start, fills the owner-summary recipients from duewise.reminders.owner-emails (or the
 * REMINDER_OWNER_EMAILS environment variable). Once any recipient exists the API owns the list and
 * this does nothing, so a change made through the API is never overwritten by a restart.
 */
@Component
class OwnerRecipientSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerRecipientSeeder.class);

    private final ReminderRecipientRepository repository;
    private final ReminderProperties properties;

    OwnerRecipientSeeder(ReminderRecipientRepository repository, ReminderProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0 || properties.ownerEmails().isEmpty()) {
            return;
        }
        repository.saveAll(properties.ownerEmails().stream()
                .map(email -> new ReminderRecipient(email, DeliveryType.TO))
                .toList());
        log.info("Seeded owner-summary recipients from configuration: {}", properties.ownerEmails());
    }
}
