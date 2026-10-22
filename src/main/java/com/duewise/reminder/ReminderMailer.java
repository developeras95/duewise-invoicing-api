package com.duewise.reminder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.duewise.config.BusinessProperties;
import com.duewise.config.MailSettings;
import com.duewise.pdf.InvoiceDocument;
import com.duewise.pdf.InvoicePdfService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/** Builds and sends the two reminder emails from Thymeleaf templates in templates/mail. */
@Component
class ReminderMailer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final ITemplateEngine templates;
    private final InvoicePdfService pdfService;
    private final MailSettings mail;
    private final BusinessProperties business;

    ReminderMailer(JavaMailSender mailSender, ITemplateEngine templates, InvoicePdfService pdfService,
                   MailSettings mail, BusinessProperties business) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.pdfService = pdfService;
        this.mail = mail;
        this.business = business;
    }

    /** "Reminder: invoice INV-2026-00042 is 7 days overdue", with the invoice PDF attached. */
    void sendClientReminder(InvoiceDocument invoice, long daysOverdue, String to) throws MessagingException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("doc", invoice);
        context.setVariable("daysOverdue", daysOverdue);
        context.setVariable("dayWord", daysOverdue == 1 ? "day" : "days");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        addressFrom(helper);
        helper.setTo(to);
        helper.setSubject("Reminder: invoice %s is %d %s overdue".formatted(
                invoice.label(), daysOverdue, daysOverdue == 1 ? "day" : "days"));
        helper.setText(templates.process("mail/client-reminder", context), true);
        helper.addAttachment(invoice.fileName(), new ByteArrayResource(pdfService.render(invoice)), "application/pdf");
        mailSender.send(message);
    }

    /** One email listing every overdue invoice, to the owner-summary recipients. */
    void sendOwnerDigest(List<InvoiceDocument> overdue, List<String> to, List<String> cc, LocalDate today)
            throws MessagingException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("invoices", overdue);
        context.setVariable("today", DATE.format(today));
        context.setVariable("businessName", business.name());

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        addressFrom(helper);
        helper.setTo(to.toArray(String[]::new));
        if (!cc.isEmpty()) {
            helper.setCc(cc.toArray(String[]::new));
        }
        helper.setSubject("%d overdue %s on %s".formatted(
                overdue.size(), overdue.size() == 1 ? "invoice" : "invoices", DATE.format(today)));
        helper.setText(templates.process("mail/owner-digest", context), true);
        mailSender.send(message);
    }

    private void addressFrom(MimeMessageHelper helper) throws MessagingException {
        helper.setFrom(mail.from());
        if (mail.replyTo() != null) {
            helper.setReplyTo(mail.replyTo());
        }
    }
}
