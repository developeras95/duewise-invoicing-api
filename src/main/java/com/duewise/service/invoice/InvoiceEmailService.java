package com.duewise.service.invoice;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.duewise.config.BusinessProperties;
import com.duewise.dto.invoice.InvoiceEmailResponse;
import com.duewise.dto.pdf.InvoiceDocument;
import com.duewise.entity.invoice.Invoice;
import com.duewise.exception.ConflictException;
import com.duewise.exception.ExternalServiceException;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

/**
 * Emails an issued invoice to its client, with the PDF attached. Sending an invoice (numbering and
 * locking it) and emailing it are separate steps, so an invoice can also be emailed again later.
 * Nothing is stored: the email is sent, or the caller is told it was not.
 */
@Service
public class InvoiceEmailService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);

    private final InvoiceService invoices;
    private final InvoiceMailer mailer;
    private final BusinessProperties business;
    private final Clock clock;

    public InvoiceEmailService(InvoiceService invoices, InvoiceMailer mailer, BusinessProperties business, Clock clock) {
        this.invoices = invoices;
        this.mailer = mailer;
        this.business = business;
        this.clock = clock;
    }

    public InvoiceEmailResponse email(Long id) {
        Invoice invoice = invoices.requireWithDetails(id);
        switch (invoice.getStatus()) {
            case DRAFT -> throw new ConflictException("INVOICE_NOT_SENT",
                    "Invoice #" + id + " is a draft; send it before emailing it.");
            case VOID -> throw new ConflictException("INVOICE_VOID",
                    "Invoice " + invoice.getInvoiceNumber() + " is void and cannot be emailed.");
            case SENT, PARTIALLY_PAID, PAID -> {
                // Paid invoices may be emailed too, as a copy for the client's records.
            }
        }

        InvoiceDocument document = InvoiceDocument.from(invoice, business, LocalDate.now(clock));
        String to = document.clientEmail();
        try {
            mailer.sendInvoice(document, to);
        } catch (MessagingException | MailException e) {
            log.warn("Emailing invoice {} to {} failed: {}", invoice.getInvoiceNumber(), to, e.getMessage());
            throw new ExternalServiceException("EMAIL_FAILED",
                    "The mail server did not accept the email, so nothing was sent. Try again later.");
        }
        log.info("Invoice {} emailed to {}", invoice.getInvoiceNumber(), to);
        return new InvoiceEmailResponse(invoice.getId(), invoice.getInvoiceNumber(), to, Instant.now(clock));
    }
}
