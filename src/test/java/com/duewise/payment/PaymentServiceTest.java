package com.duewise.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.ConflictException;
import com.duewise.currency.Currency;
import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceService;
import com.duewise.invoice.InvoiceStatus;
import com.duewise.paymentmethod.PaymentMethod;
import com.duewise.paymentmethod.PaymentMethodService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T05:00:00Z"), ZoneId.of("Asia/Kolkata"));
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final PaymentMethod UPI = new PaymentMethod("UPI", "UPI", true, (short) 20);
    private static final PaymentMethod CASH = new PaymentMethod("CASH", "Cash", false, (short) 50);

    @Mock PaymentRepository repository;
    @Mock InvoiceService invoiceService;
    @Mock PaymentMethodService paymentMethodService;
    @Mock Invoice invoice;

    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(repository, invoiceService, paymentMethodService, CLOCK);
        when(invoiceService.require(1L)).thenReturn(invoice);
    }

    @Test
    void invoiceThatTakesNoPaymentsIsRefusedFirst() {
        doThrow(new ConflictException("INVOICE_NOT_SENT", "Invoice #1 is a draft; send it before recording payments."))
                .when(invoice).ensureAcceptsPayment();

        assertThatThrownBy(() -> service.record(1L, request("100", "CASH", null, null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(paymentMethodService, repository);
    }

    @Test
    void methodThatNeedsAReferenceRefusesABlankOne() {
        when(paymentMethodService.requireActive("UPI")).thenReturn(UPI);

        assertThatThrownBy(() -> service.record(1L, request("100", "UPI", null, "   ")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reference is required for UPI");
        verify(invoice, never()).recordPayment(any(), any());
    }

    @Test
    void paymentDatedInTheFutureIsRefused() {
        when(paymentMethodService.requireActive("CASH")).thenReturn(CASH);

        assertThatThrownBy(() -> service.record(1L, request("100", "CASH", TODAY.plusDays(1), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void paymentDatedBeforeTheIssueDateIsRefused() {
        when(paymentMethodService.requireActive("CASH")).thenReturn(CASH);
        when(invoice.getIssueDate()).thenReturn(TODAY);

        assertThatThrownBy(() -> service.record(1L, request("100", "CASH", TODAY.minusDays(1), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("issue date");
    }

    @Test
    void validPaymentUpdatesTheInvoiceAndIsSavedDatedToday() {
        when(paymentMethodService.requireActive("CASH")).thenReturn(CASH);
        when(invoice.getIssueDate()).thenReturn(TODAY.minusDays(10));
        when(invoice.getCurrency()).thenReturn(new Currency("INR", "Indian Rupee", "₹", (short) 2));
        when(invoice.getTotal()).thenReturn(new BigDecimal("1000.000"));
        when(invoice.getAmountPaid()).thenReturn(new BigDecimal("400.000"));
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("600.000"));
        when(invoice.getStatus()).thenReturn(InvoiceStatus.PARTIALLY_PAID);
        when(repository.saveAndFlush(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        PaymentReceipt receipt = service.record(1L, request("400", "CASH", null, null));

        verify(invoice).recordPayment(new BigDecimal("400"), Instant.now(CLOCK));
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPaidOn()).isEqualTo(TODAY);
        assertThat(receipt.invoice().status()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(receipt.invoice().balanceDue()).isEqualByComparingTo("600.00");
        assertThat(receipt.payment().amount().toPlainString()).isEqualTo("400.00");
    }

    private static PaymentRequest request(String amount, String method, LocalDate paidOn, String reference) {
        return new PaymentRequest(new BigDecimal(amount), method, paidOn, reference, null);
    }
}
