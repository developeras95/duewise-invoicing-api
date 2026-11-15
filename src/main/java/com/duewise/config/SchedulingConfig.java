package com.duewise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on @Scheduled jobs. Each job has its own switch: duewise.reminders.scheduler-enabled for the
 * daily reminders and duewise.reports.refresh-enabled for the nightly revenue report refresh. The tests
 * turn both off, so that no job can fire in the middle of a test run.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
