package com.duewise.service.reminder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the reminders on the configured schedule (duewise.reminders.cron, 09:00 every day by default)
 * in the business time zone. Switched off with duewise.reminders.scheduler-enabled=false.
 */
@Component
@ConditionalOnProperty(prefix = "duewise.reminders", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReminderJob {

    private final ReminderService service;

    public ReminderJob(ReminderService service) {
        this.service = service;
    }

    @Scheduled(cron = "${duewise.reminders.cron}", zone = "${duewise.business.time-zone}")
    void runDaily() {
        service.run();
    }
}
