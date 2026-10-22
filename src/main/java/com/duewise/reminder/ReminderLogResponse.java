package com.duewise.reminder;

import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReminderLogEntry")
public record ReminderLogResponse(
        Long id,
        ReminderLog.Type type,
        @Schema(description = "Client reminders only") Long invoiceId,
        @Schema(description = "Client reminders only: the reminder day", example = "7") Short reminderDay,
        @Schema(description = "Owner summaries only") LocalDate digestDate,
        String recipients,
        ReminderLog.Status status,
        int attempts,
        String lastError,
        Instant sentAt,
        Instant updatedAt) {

    static ReminderLogResponse from(ReminderLog log) {
        return new ReminderLogResponse(log.getId(), log.getReminderType(), log.getInvoiceId(), log.getDaysOverdue(),
                log.getDigestDate(), log.getRecipients(), log.getStatus(), log.getAttempts(), log.getLastError(),
                log.getSentAt(), log.getUpdatedAt());
    }
}
