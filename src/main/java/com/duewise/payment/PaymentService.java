package com.duewise.payment;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.duewise.common.error.BadRequestException;
import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceService;
import com.duewise.paymentmethod.PaymentMethod;
import com.duewise.paymentmethod.PaymentMethodService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository repository;
    private final InvoiceService invoiceService;
    private final PaymentMethodService paymentMethodService;
    private final Clock clock;

    public PaymentService(PaymentRepository repository, InvoiceService invoiceService,
                          PaymentMethodService paymentMethodService, Clock clock) {
        this.repository = repository;
        this.invoiceService = invoiceService;
        this.paymentMethodService = paymentMethodService;
        this.clock = clock;
    }

    /**
     * Records a payment and updates the invoice in the same transaction: either both change or
     * neither does. Two payments racing on one invoice are caught by the invoice's version (409).
     */
    @Transactional
    public PaymentReceipt record(Long invoiceId, PaymentRequest request) {
        Invoice invoice = invoiceService.require(invoiceId);
        invoice.ensureAcceptsPayment();

        PaymentMethod method = paymentMethodService.requireActive(request.paymentMethod());
        String reference = trimToNull(request.reference());
        if (method.isRequiresReference() && reference == null) {
            throw new BadRequestException("REFERENCE_REQUIRED",
                    "A reference is required for " + method.getDisplayName() + " payments.");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate paidOn = request.paidOn() != null ? request.paidOn() : today;
        if (paidOn.isAfter(today)) {
            throw new BadRequestException("INVALID_PAYMENT_DATE", "paidOn cannot be in the future.");
        }
        if (paidOn.isBefore(invoice.getIssueDate())) {
            throw new BadRequestException("INVALID_PAYMENT_DATE",
                    "paidOn cannot be before the invoice's issue date (" + invoice.getIssueDate() + ").");
        }

        invoice.recordPayment(request.amount(), Instant.now(clock));
        Payment payment = new Payment(invoice, method, request.amount(), paidOn, reference, trimToNull(request.notes()));
        // Flush now so the receipt carries the invoice's new version.
        return PaymentReceipt.of(repository.saveAndFlush(payment), invoice);
    }

    public List<PaymentResponse> list(Long invoiceId) {
        Invoice invoice = invoiceService.require(invoiceId);
        int scale = invoice.getCurrency().getMinorUnits();
        return repository.findByInvoiceIdOrderByPaidOnAscIdAsc(invoiceId).stream()
                .map(payment -> PaymentResponse.from(payment, scale))
                .toList();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
