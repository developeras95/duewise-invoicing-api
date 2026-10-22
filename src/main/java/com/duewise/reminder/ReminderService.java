package com.duewise.reminder;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.duewise.common.web.PageResponse;
import com.duewise.config.BusinessProperties;
import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceService;
import com.duewise.pdf.InvoiceDocument;
import com.duewise.reminder.ReminderLog.Status;
import com.duewise.reminder.ReminderLog.Type;
import com.duewise.reminder.ReminderRunSummary.OwnerDigestOutcome;
import com.duewise.reminder.ReminderSettingsService.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily reminder run. Not transactional itself: it reads in short read-only transactions, sends
 * each email, and the log writer records each attempt in its own transaction. A slow or failing mail
 * server therefore never holds a database transaction open.
 */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final ReminderSettingsService settingsService;
    private final InvoiceService invoiceService;
    private final ReminderLogRepository logRepository;
    private final ReminderLogWriter logWriter;
    private final ReminderMailer mailer;
    private final BusinessProperties business;
    private final Clock clock;

    public ReminderService(ReminderSettingsService settingsService, InvoiceService invoiceService,
                           ReminderLogRepository logRepository, ReminderLogWriter logWriter, ReminderMailer mailer,
                           BusinessProperties business, Clock clock) {
        this.settingsService = settingsService;
        this.invoiceService = invoiceService;
        this.logRepository = logRepository;
        this.logWriter = logWriter;
        this.mailer = mailer;
        this.business = business;
        this.clock = clock;
    }

    public ReminderRunSummary run() {
        LocalDate today = LocalDate.now(clock);
        Snapshot settings = settingsService.snapshot();
        List<Invoice> overdue = invoiceService.findOverdue(today);

        int sent = 0;
        int failed = 0;
        int skipped = 0;
        if (settings.clientRemindersEnabled()) {
            Set<String> alreadySent = alreadySent(overdue);
            for (Invoice invoice : overdue) {
                long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), today);
                Optional<Integer> due = ReminderSchedule.dueReminder(daysOverdue, settings.reminderDays());
                if (!invoice.getClient().isRemindersEnabled() || due.isEmpty()
                        || alreadySent.contains(key(invoice.getId(), due.get()))) {
                    skipped++;
                    continue;
                }
                short reminderDay = due.get().shortValue();
                String to = invoice.getClient().reminderEmail();
                try {
                    mailer.sendClientReminder(InvoiceDocument.from(invoice, business, today), daysOverdue, to);
                    logWriter.clientReminder(invoice.getId(), reminderDay, to, Status.SENT, null);
                    sent++;
                } catch (Exception e) {
                    log.warn("Reminder for invoice {} to {} failed: {}", invoice.getInvoiceNumber(), to, e.getMessage());
                    logWriter.clientReminder(invoice.getId(), reminderDay, to, Status.FAILED, e.getMessage());
                    failed++;
                }
            }
        }

        OwnerDigestOutcome digest = sendOwnerDigest(settings, overdue, today);
        ReminderRunSummary summary = new ReminderRunSummary(today, overdue.size(), sent, failed, skipped, digest);
        log.info("Reminder run: {}", summary);
        return summary;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderLogResponse> history(Pageable pageable) {
        return PageResponse.from(logRepository.findAllByOrderByUpdatedAtDescIdDesc(pageable), ReminderLogResponse::from);
    }

    private OwnerDigestOutcome sendOwnerDigest(Snapshot settings, List<Invoice> overdue, LocalDate today) {
        if (!settings.ownerDigestEnabled()) {
            return OwnerDigestOutcome.DISABLED;
        }
        if (settings.to().isEmpty()) {
            return OwnerDigestOutcome.NO_RECIPIENTS;
        }
        boolean sentToday = logRepository.findByReminderTypeAndDigestDate(Type.OWNER_DIGEST, today)
                .map(entry -> entry.getStatus() == Status.SENT)
                .orElse(false);
        if (sentToday) {
            return OwnerDigestOutcome.ALREADY_SENT_TODAY;
        }
        if (overdue.isEmpty()) {
            return OwnerDigestOutcome.NOTHING_OVERDUE;
        }

        String recipients = String.join(", ", settings.to())
                + (settings.cc().isEmpty() ? "" : " (cc " + String.join(", ", settings.cc()) + ")");
        List<InvoiceDocument> documents = overdue.stream()
                .map(invoice -> InvoiceDocument.from(invoice, business, today))
                .toList();
        try {
            mailer.sendOwnerDigest(documents, settings.to(), settings.cc(), today);
            logWriter.ownerDigest(today, recipients, Status.SENT, null);
            return OwnerDigestOutcome.SENT;
        } catch (Exception e) {
            log.warn("Owner summary for {} failed: {}", today, e.getMessage());
            logWriter.ownerDigest(today, recipients, Status.FAILED, e.getMessage());
            return OwnerDigestOutcome.FAILED;
        }
    }

    private Set<String> alreadySent(List<Invoice> overdue) {
        if (overdue.isEmpty()) {
            return Set.of();
        }
        List<Long> ids = overdue.stream().map(Invoice::getId).toList();
        Set<String> keys = new HashSet<>();
        for (ReminderLog entry : logRepository.findByReminderTypeAndStatusAndInvoice_IdIn(Type.CLIENT_REMINDER, Status.SENT, ids)) {
            keys.add(key(entry.getInvoiceId(), entry.getDaysOverdue()));
        }
        return keys;
    }

    private static String key(Long invoiceId, int reminderDay) {
        return invoiceId + ":" + reminderDay;
    }
}
