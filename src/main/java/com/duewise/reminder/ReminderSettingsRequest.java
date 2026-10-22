package com.duewise.reminder;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Replaces all reminder settings at once. Send the version from your last GET. */
public record ReminderSettingsRequest(

        @Schema(description = "Daily summary of every overdue invoice to the recipients below", example = "true")
        @NotNull
        Boolean ownerDigestEnabled,

        @Schema(description = "Reminders to each client on the reminder days, invoice PDF attached", example = "true")
        @NotNull
        Boolean clientRemindersEnabled,

        @Schema(description = "Who receives the owner summary. Each email once, ignoring case.")
        @NotNull
        @Size(max = 20)
        List<@NotNull @Valid Recipient> recipients,

        @Schema(description = "Days past the due date on which clients are reminded", example = "[1, 7, 14]")
        @NotNull
        @Size(min = 1, max = 10)
        List<@NotNull @Min(1) @Max(365) Integer> reminderDays,

        @Schema(description = "The version you last read", example = "0")
        @NotNull
        Long version) {

    @Schema(name = "ReminderRecipient")
    public record Recipient(
            @Schema(example = "owner@acmedesign.in") @NotBlank @Email @Size(max = 254) String email,
            @Schema(example = "TO") @NotNull DeliveryType type) {
    }
}
