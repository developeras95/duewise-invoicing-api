package com.duewise.service.invoice;

import java.util.Locale;

import com.duewise.config.MailSettings;
import com.duewise.dto.pdf.InvoiceDocument;
import com.duewise.service.pdf.InvoicePdfService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/** Builds and sends the "here is your invoice" email from templates/mail/invoice.html. */
@Component
public class InvoiceMailer {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templates;
    private final InvoicePdfService pdfService;
    private final MailSettings mail;

    public InvoiceMailer(JavaMailSender mailSender, ITemplateEngine templates, InvoicePdfService pdfService,
                         MailSettings mail) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.pdfService = pdfService;
        this.mail = mail;
    }

    /** "Invoice INV-2026-00042 from Acme Studio", with the invoice PDF attached. */
    public void sendInvoice(InvoiceDocument invoice, String to) throws MessagingException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("doc", invoice);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mail.from());
        if (mail.replyTo() != null) {
            helper.setReplyTo(mail.replyTo());
        }
        helper.setTo(to);
        helper.setSubject("Invoice %s from %s".formatted(invoice.label(), invoice.businessName()));
        helper.setText(templates.process("mail/invoice", context), true);
        helper.addAttachment(invoice.fileName(), new ByteArrayResource(pdfService.render(invoice)), "application/pdf");
        mailSender.send(message);
    }
}
