package com.duewise.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReminderScheduleTest {

    private static final List<Integer> DAYS = List.of(1, 7, 14);

    @Test
    void nothingIsDueBeforeTheFirstReminderDay() {
        assertThat(ReminderSchedule.dueReminder(0, DAYS)).isEmpty();
    }

    @Test
    void eachReminderDayIsDueOnTheDay() {
        assertThat(ReminderSchedule.dueReminder(1, DAYS)).contains(1);
        assertThat(ReminderSchedule.dueReminder(7, DAYS)).contains(7);
        assertThat(ReminderSchedule.dueReminder(14, DAYS)).contains(14);
    }

    @Test
    void missedDaysCatchUpWithTheMostRecentReminderOnly() {
        assertThat(ReminderSchedule.dueReminder(10, DAYS)).contains(7);
        assertThat(ReminderSchedule.dueReminder(90, DAYS)).contains(14);
    }
}
