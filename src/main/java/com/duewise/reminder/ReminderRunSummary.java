package com.duewise.reminder;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/** What one reminder run did. */
@Schema(name = "ReminderRunSummary")
public record ReminderRunSummary(
        LocalDate runDate,
        @Schema(description = "Overdue invoices looked at", example = "4") int overdueInvoices,
        @Schema(example = "2") int clientRemindersSent,
        @Schema(example = "0") int clientRemindersFailed,
        @Schema(description = "Already sent earlier, client opted out, or no reminder day reached yet", example = "2")
        int clientRemindersSkipped,
        OwnerDigestOutcome ownerDigest) {

    public enum OwnerDigestOutcome {
        SENT,
        FAILED,
        ALREADY_SENT_TODAY,
        NOTHING_OVERDUE,
        NO_RECIPIENTS,
        DISABLED
    }
}
