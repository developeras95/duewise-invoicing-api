package com.duewise.reminder;

import java.util.List;
import java.util.Optional;

/**
 * Which reminder an overdue invoice is due for today. Pure logic, no Spring, so the rule is tested
 * directly.
 *
 * <p>The answer is the largest reminder day the invoice has reached. If the job did not run for a
 * while (server down over a long weekend), the next run catches up with the most recent reminder
 * only: an invoice now 10 days overdue gets the day-7 reminder, not day 1 and day 7 together.
 */
final class ReminderSchedule {

    private ReminderSchedule() {
    }

    /** @param reminderDays ascending, e.g. [1, 7, 14] */
    static Optional<Integer> dueReminder(long daysOverdue, List<Integer> reminderDays) {
        Integer due = null;
        for (Integer day : reminderDays) {
            if (day <= daysOverdue) {
                due = day;
            }
        }
        return Optional.ofNullable(due);
    }
}
