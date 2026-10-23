package com.duewise.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.duewise.config.BusinessProperties;
import com.duewise.invoice.InvoiceService;
import com.duewise.reminder.ReminderLog.Status;
import com.duewise.reminder.ReminderLog.Type;
import com.duewise.reminder.ReminderRunSummary.OwnerDigestOutcome;
import com.duewise.reminder.ReminderSettingsService.Snapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The owner-summary decisions of a reminder run, without a database or a mail server.
 * Sending itself is covered end to end in PdfAndReminderIntegrationTest.
 */
class ReminderServiceTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private final ReminderSettingsService settings = mock(ReminderSettingsService.class);
    private final InvoiceService invoices = mock(InvoiceService.class);
    private final ReminderLogRepository logRepository = mock(ReminderLogRepository.class);
    private final ReminderLogWriter logWriter = mock(ReminderLogWriter.class);
    private final ReminderMailer mailer = mock(ReminderMailer.class);

    private ReminderService service;

    @BeforeEach
    void setUp() {
        BusinessProperties business = new BusinessProperties("Asia/Kolkata", "Duewise Demo Studio", List.of(), null, null);
        Clock clock = Clock.fixed(TODAY.atTime(9, 0).atZone(KOLKATA).toInstant(), KOLKATA);
        service = new ReminderService(settings, invoices, logRepository, logWriter, mailer, business, clock);
        when(invoices.findOverdue(TODAY)).thenReturn(List.of());
        when(logRepository.findByReminderTypeAndDigestDate(Type.OWNER_DIGEST, TODAY)).thenReturn(Optional.empty());
    }

    @Test
    void ownerSummaryCanBeSwitchedOff() {
        when(settings.snapshot()).thenReturn(snapshot(false, List.of("owner@acme.in")));

        ReminderRunSummary summary = service.run();

        assertThat(summary.ownerDigest()).isEqualTo(OwnerDigestOutcome.DISABLED);
        verifyNoInteractions(mailer);
    }

    @Test
    void ownerSummaryNeedsAtLeastOneToRecipient() {
        when(settings.snapshot()).thenReturn(snapshot(true, List.of()));

        assertThat(service.run().ownerDigest()).isEqualTo(OwnerDigestOutcome.NO_RECIPIENTS);
        verifyNoInteractions(mailer);
    }

    @Test
    void noEmailWhenNothingIsOverdue() throws Exception {
        when(settings.snapshot()).thenReturn(snapshot(true, List.of("owner@acme.in")));

        ReminderRunSummary summary = service.run();

        assertThat(summary.ownerDigest()).isEqualTo(OwnerDigestOutcome.NOTHING_OVERDUE);
        assertThat(summary.overdueInvoices()).isZero();
        assertThat(summary.clientRemindersSent()).isZero();
        verify(mailer, never()).sendOwnerDigest(anyList(), anyList(), anyList(), any());
    }

    @Test
    void onlyOneOwnerSummaryPerDay() {
        when(settings.snapshot()).thenReturn(snapshot(true, List.of("owner@acme.in")));
        when(logRepository.findByReminderTypeAndDigestDate(Type.OWNER_DIGEST, TODAY))
                .thenReturn(Optional.of(digestLog(Status.SENT)));

        assertThat(service.run().ownerDigest()).isEqualTo(OwnerDigestOutcome.ALREADY_SENT_TODAY);
        verifyNoInteractions(mailer);
    }

    @Test
    void aFailedSummaryEarlierTodayDoesNotBlockTheNextRun() {
        when(settings.snapshot()).thenReturn(snapshot(true, List.of("owner@acme.in")));
        when(logRepository.findByReminderTypeAndDigestDate(Type.OWNER_DIGEST, TODAY))
                .thenReturn(Optional.of(digestLog(Status.FAILED)));

        // Not ALREADY_SENT_TODAY: the run gets past the "sent today" check and only stops
        // because there is nothing overdue to report.
        assertThat(service.run().ownerDigest()).isEqualTo(OwnerDigestOutcome.NOTHING_OVERDUE);
    }

    private static Snapshot snapshot(boolean ownerDigestEnabled, List<String> to) {
        return new Snapshot(ownerDigestEnabled, true, to, List.of(), List.of(1, 7, 14));
    }

    private static ReminderLog digestLog(Status status) {
        ReminderLog log = ReminderLog.ownerDigest(TODAY);
        log.recordAttempt(status, "owner@acme.in", status == Status.FAILED ? "Connection refused" : null,
                Instant.parse("2026-10-22T03:30:00Z"));
        return log;
    }
}
