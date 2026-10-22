package com.duewise.reminder;

import java.time.Instant;
import java.util.List;

import com.duewise.reminder.ReminderSettingsRequest.Recipient;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReminderSettings")
public record ReminderSettingsResponse(
        boolean ownerDigestEnabled,
        boolean clientRemindersEnabled,
        List<Recipient> recipients,
        @Schema(example = "[1, 7, 14]") List<Integer> reminderDays,
        Instant updatedAt,
        @Schema(description = "Send this back when updating", example = "0") long version) {
}
