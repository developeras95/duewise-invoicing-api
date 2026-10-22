package com.duewise.reminder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.duewise.invoice.InvoiceRepository;
import com.duewise.reminder.ReminderLog.Status;
import com.duewise.reminder.ReminderLog.Type;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes each reminder attempt in its own transaction, straight after the email is sent or fails.
 * One bad address therefore never undoes the log rows of the emails that did go out.
 */
@Component
class ReminderLogWriter {

    private final ReminderLogRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    ReminderLogWriter(ReminderLogRepository repository, InvoiceRepository invoiceRepository, Clock clock) {
        this.repository = repository;
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clientReminder(Long invoiceId, short daysOverdue, String recipient, Status status, String error) {
        ReminderLog log = repository.findByReminderTypeAndInvoice_IdAndDaysOverdue(Type.CLIENT_REMINDER, invoiceId, daysOverdue)
                .orElseGet(() -> ReminderLog.clientReminder(invoiceRepository.getReferenceById(invoiceId), daysOverdue));
        log.recordAttempt(status, recipient, error, Instant.now(clock));
        repository.save(log);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ownerDigest(LocalDate date, String recipients, Status status, String error) {
        ReminderLog log = repository.findByReminderTypeAndDigestDate(Type.OWNER_DIGEST, date)
                .orElseGet(() -> ReminderLog.ownerDigest(date));
        log.recordAttempt(status, recipients, error, Instant.now(clock));
        repository.save(log);
    }
}
