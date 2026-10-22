package com.duewise.reminder;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.ConflictException;
import com.duewise.reminder.ReminderSettingsRequest.Recipient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReminderSettingsService {

    /** What the reminder job needs, read once per run. */
    record Snapshot(boolean ownerDigestEnabled, boolean clientRemindersEnabled,
                    List<String> to, List<String> cc, List<Integer> reminderDays) {
    }

    private final ReminderSettingsRepository settingsRepository;
    private final ReminderRecipientRepository recipientRepository;
    private final ReminderOffsetRepository offsetRepository;
    private final Clock clock;

    public ReminderSettingsService(ReminderSettingsRepository settingsRepository,
                                   ReminderRecipientRepository recipientRepository,
                                   ReminderOffsetRepository offsetRepository, Clock clock) {
        this.settingsRepository = settingsRepository;
        this.recipientRepository = recipientRepository;
        this.offsetRepository = offsetRepository;
        this.clock = clock;
    }

    public ReminderSettingsResponse get() {
        return toResponse(settings());
    }

    Snapshot snapshot() {
        ReminderSettings settings = settings();
        List<ReminderRecipient> recipients = recipientRepository.findAllByOrderByDeliveryTypeAscEmailAsc();
        return new Snapshot(
                settings.isOwnerDigestEnabled(),
                settings.isClientRemindersEnabled(),
                recipients.stream().filter(r -> r.getDeliveryType() == DeliveryType.TO).map(ReminderRecipient::getEmail).toList(),
                recipients.stream().filter(r -> r.getDeliveryType() == DeliveryType.CC).map(ReminderRecipient::getEmail).toList(),
                reminderDays());
    }

    /**
     * Replaces switches, recipients and reminder days in one transaction. The old recipient and day
     * rows are deleted with one statement each before the new ones are inserted, so an email that
     * stays on the list never clashes with its own unique index.
     */
    @Transactional
    public ReminderSettingsResponse update(ReminderSettingsRequest request) {
        ReminderSettings settings = settings();
        if (settings.getVersion() != request.version()) {
            throw new ConflictException("CONCURRENT_MODIFICATION",
                    "Reminder settings were changed by someone else (now at version " + settings.getVersion()
                            + "). Reload them and try again.");
        }
        checkNoDuplicates(request);

        recipientRepository.deleteAllInBatch();
        offsetRepository.deleteAllInBatch();
        recipientRepository.saveAll(request.recipients().stream()
                .map(r -> new ReminderRecipient(r.email().trim(), r.type()))
                .toList());
        offsetRepository.saveAll(request.reminderDays().stream()
                .map(days -> new ReminderOffset(days.shortValue()))
                .toList());

        settings.update(request.ownerDigestEnabled(), request.clientRemindersEnabled(), Instant.now(clock));
        return toResponse(settingsRepository.saveAndFlush(settings));
    }

    private static void checkNoDuplicates(ReminderSettingsRequest request) {
        Set<String> emails = new HashSet<>();
        for (Recipient recipient : request.recipients()) {
            if (!emails.add(recipient.email().trim().toLowerCase(Locale.ROOT))) {
                throw new BadRequestException("DUPLICATE_RECIPIENT",
                        recipient.email().trim() + " appears more than once in recipients.");
            }
        }
        if (new HashSet<>(request.reminderDays()).size() != request.reminderDays().size()) {
            throw new BadRequestException("DUPLICATE_REMINDER_DAY", "Each reminder day may appear only once.");
        }
    }

    private ReminderSettings settings() {
        return settingsRepository.findById(ReminderSettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("reminder_settings row is missing; migration V4 seeds it"));
    }

    private List<Integer> reminderDays() {
        return offsetRepository.findAllByOrderByDaysOverdueAsc().stream()
                .map(offset -> (int) offset.getDaysOverdue())
                .toList();
    }

    private ReminderSettingsResponse toResponse(ReminderSettings settings) {
        List<Recipient> recipients = recipientRepository.findAllByOrderByDeliveryTypeAscEmailAsc().stream()
                .map(r -> new Recipient(r.getEmail(), r.getDeliveryType()))
                .toList();
        return new ReminderSettingsResponse(settings.isOwnerDigestEnabled(), settings.isClientRemindersEnabled(),
                recipients, reminderDays(), settings.getUpdatedAt(), settings.getVersion());
    }
}
