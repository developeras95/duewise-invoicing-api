package com.duewise.reminder;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the reminders on the configured schedule (duewise.reminders.cron, 09:00 every day by default)
 * in the business time zone. Only active when scheduling is enabled; see SchedulingConfig.
 */
@Component
class ReminderJob {

    private final ReminderService service;

    ReminderJob(ReminderService service) {
        this.service = service;
    }

    @Scheduled(cron = "${duewise.reminders.cron}", zone = "${duewise.business.time-zone}")
    void runDaily() {
        service.run();
    }
}
