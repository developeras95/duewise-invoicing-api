package com.duewise.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled jobs. Switched off with duewise.reminders.scheduler-enabled=false, which the
 * tests do so that a job can never fire in the middle of a test run.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "duewise.reminders", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
