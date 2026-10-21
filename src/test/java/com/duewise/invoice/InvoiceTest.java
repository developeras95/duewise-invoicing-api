package com.duewise.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.duewise.client.Client;
import com.duewise.common.error.BusinessRuleException;
import com.duewise.common.error.ConflictException;
import com.duewise.currency.Currency;
import org.junit.jupiter.api.Test;

/** The invoice's own rules, tested without Spring or a database. */
class InvoiceTest {

    private static final Currency INR = new Currency("INR", "Indian Rupee", "₹", (short) 2);
    private static final LocalDate ISSUED = LocalDate.of(2026, 10, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 10, 31);
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    @Test
    void newInvoiceIsADraftWhoseTotalsComeFromItsLines() {
        Invoice invoice = draftWith(line("2", "12500.50", "18"), line("1.5", "999.99", "0"));

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(invoice.getInvoiceNumber()).isNull();
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("26500.99");
        assertThat(invoice.getTaxTotal()).isEqualByComparingTo("4500.18");
        assertThat(invoice.getTotal()).isEqualByComparingTo("31001.17");
        assertThat(invoice.getBalanceDue()).isEqualByComparingTo("31001.17");
    }

    @Test
    void replacingLinesRemovesSurplusOnesAndRecalculates() {
        Invoice invoice = draftWith(line("1", "100", "0"), line("1", "200", "0"), line("1", "300", "0"));

        invoice.replaceLines(List.of(line("2", "50", "0")), 2);

        assertThat(invoice.getLines()).hasSize(1);
        assertThat(invoice.getLines().getFirst().getLineNo()).isEqualTo((short) 1);
        assertThat(invoice.getTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void sendingAssignsTheNumberAndLocksTheInvoice() {
        Invoice invoice = draftWith(line("1", "1000", "18"));

        invoice.send(() -> "INV-2026-00001", NOW);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.SENT);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-00001");
        assertThat(invoice.getSentAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> invoice.replaceLines(List.of(line("1", "1", "0")), 2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only drafts can be changed");
        assertThatThrownBy(invoice::ensureDeletable).isInstanceOf(ConflictException.class);
    }

    @Test
    void emptyDraftCannotBeSentAndDoesNotTakeANumber() {
        Invoice invoice = draftWith();
        AtomicInteger numbersTaken = new AtomicInteger();

        assertThatThrownBy(() -> invoice.send(() -> "INV-" + numbersTaken.incrementAndGet(), NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one line");
        assertThat(numbersTaken).hasValue(0);
    }

    @Test
    void sentInvoiceCannotBeSentAgain() {
        Invoice invoice = draftWith(line("1", "1000", "0"));
        invoice.send(() -> "INV-2026-00001", NOW);

        assertThatThrownBy(() -> invoice.send(() -> "INV-2026-00002", NOW)).isInstanceOf(ConflictException.class);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2026-00001");
    }

    @Test
    void sentInvoiceCanBeVoidedOnce() {
        Invoice invoice = draftWith(line("1", "1000", "0"));
        invoice.send(() -> "INV-2026-00001", NOW);

        invoice.voidInvoice("Wrong client", NOW);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VOID);
        assertThat(invoice.getVoidReason()).isEqualTo("Wrong client");
        assertThatThrownBy(() -> invoice.voidInvoice("Again", NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already void");
    }

    @Test
    void draftIsDeletedNotVoided() {
        Invoice invoice = draftWith(line("1", "1000", "0"));

        assertThatThrownBy(() -> invoice.voidInvoice("No longer needed", NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("deleted, not voided");
    }

    @Test
    void overdueOnlyWhenMoneyIsOwedAndDueDateHasPassed() {
        Invoice invoice = draftWith(line("1", "1000", "0"));
        LocalDate afterDueDate = DUE.plusDays(1);

        assertThat(invoice.isOverdue(afterDueDate)).as("drafts are never overdue").isFalse();

        invoice.send(() -> "INV-2026-00001", NOW);
        assertThat(invoice.isOverdue(DUE)).as("due today is not overdue yet").isFalse();
        assertThat(invoice.isOverdue(afterDueDate)).isTrue();

        invoice.voidInvoice("Cancelled", NOW);
        assertThat(invoice.isOverdue(afterDueDate)).as("void invoices are never overdue").isFalse();
    }

    private static Invoice draftWith(LineDraft... lines) {
        Invoice invoice = new Invoice(mock(Client.class), INR, ISSUED, DUE, null);
        invoice.replaceLines(List.of(lines), INR.getMinorUnits());
        return invoice;
    }

    private static LineDraft line(String quantity, String unitPrice, String taxRate) {
        return new LineDraft("Work", new BigDecimal(quantity), new BigDecimal(unitPrice), new BigDecimal(taxRate));
    }
}
