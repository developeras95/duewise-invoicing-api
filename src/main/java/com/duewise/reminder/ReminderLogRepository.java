package com.duewise.reminder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.duewise.reminder.ReminderLog.Status;
import com.duewise.reminder.ReminderLog.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * "Invoice_Id" (with the underscore) tells Spring Data to follow the invoice relationship to its id.
 * Without it, "InvoiceId" would match ReminderLog.getInvoiceId(), which is a convenience getter, not
 * a mapped attribute, and Hibernate would reject the query.
 */
interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    Optional<ReminderLog> findByReminderTypeAndInvoice_IdAndDaysOverdue(Type type, Long invoiceId, Short daysOverdue);

    /** Reminders already delivered for these invoices, so a rerun never sends them again. */
    List<ReminderLog> findByReminderTypeAndStatusAndInvoice_IdIn(Type type, Status status, Collection<Long> invoiceIds);

    Optional<ReminderLog> findByReminderTypeAndDigestDate(Type type, LocalDate digestDate);

    Page<ReminderLog> findAllByOrderByUpdatedAtDescIdDesc(Pageable pageable);
}
