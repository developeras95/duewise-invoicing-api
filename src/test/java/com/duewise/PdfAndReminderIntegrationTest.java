package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PDF invoices and reminder emails end to end: real PostgreSQL, and GreenMail as an in-memory SMTP
 * server that captures every email so the test can open it.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.mail.host=127.0.0.1", "spring.mail.port=3025"})
class PdfAndReminderIntegrationTest extends AbstractIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    @Test
    void invoicePdfShowsNumberClientAndAmounts() throws Exception {
        LocalDate today = LocalDate.now(clock);
        long clientId = createClient("Kaveri Exports Pvt Ltd", "accounts+" + UUID.randomUUID() + "@example.com");
        long invoiceId = createInvoice(clientId, today, today.plusDays(30), "2", "12500.50", "18");
        String number = send(invoiceId);

        byte[] pdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"" + number + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        String text = textOf(pdf);
        assertThat(text)
                .contains(number)
                .contains("Kaveri Exports Pvt Ltd")
                .contains("INR 25,001.00")
                .contains("INR 29,501.18")
                .contains("Duewise Demo Studio");
    }

    @Test
    void reminderRunEmailsClientsAndOwnerOnceOnly() throws Exception {
        LocalDate today = LocalDate.now(clock);
        updateSettings(true, "owner@duewise.test", "accountant@duewise.test");

        String clientEmail = "accounts+" + UUID.randomUUID() + "@example.com";
        long clientId = createClient("Godavari Foods", clientEmail);
        long tenDaysLate = createInvoice(clientId, today.minusDays(40), today.minusDays(10), "1", "1000", "0");
        long twoDaysLate = createInvoice(clientId, today.minusDays(5), today.minusDays(2), "1", "500", "0");
        long notDue = createInvoice(clientId, today, today.plusDays(10), "1", "800", "0");
        String tenDaysNumber = send(tenDaysLate);
        String twoDaysNumber = send(twoDaysLate);
        send(notDue);

        mvc.perform(post("/api/v1/reminders/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerDigest").value("SENT"));

        // Two reminders to our client, one per overdue invoice, each with its PDF attached.
        List<MimeMessage> toClient = messagesTo(clientEmail);
        assertThat(toClient).extracting(MimeMessage::getSubject).containsExactlyInAnyOrder(
                "Reminder: invoice " + tenDaysNumber + " is 10 days overdue",
                "Reminder: invoice " + twoDaysNumber + " is 2 days overdue");
        for (MimeMessage message : toClient) {
            assertThat(hasPdfAttachment(message)).as("PDF attached to " + message.getSubject()).isTrue();
        }

        // One owner summary, TO and CC as configured, listing both overdue invoices.
        List<MimeMessage> toOwner = messagesTo("owner@duewise.test");
        assertThat(toOwner).hasSize(1);
        assertThat(Arrays.stream(toOwner.getFirst().getRecipients(Message.RecipientType.CC)).map(Address::toString))
                .containsExactly("accountant@duewise.test");
        String summary = GreenMailUtil.getBody(toOwner.getFirst());
        assertThat(summary).contains(tenDaysNumber).contains(twoDaysNumber).contains("Godavari Foods");

        // A second run the same day sends nothing new.
        greenMail.reset();
        mvc.perform(post("/api/v1/reminders/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerDigest").value("ALREADY_SENT_TODAY"));
        assertThat(messagesTo(clientEmail)).isEmpty();
        assertThat(messagesTo("owner@duewise.test")).isEmpty();

        mvc.perform(get("/api/v1/reminders/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SENT"));
    }

    @Test
    void draftPaidAndVoidInvoicesCarryTheirStampOnThePdf() throws Exception {
        LocalDate today = LocalDate.now(clock);
        long clientId = createClient("Narmada Textiles", "accounts+" + UUID.randomUUID() + "@example.com");

        long draft = createInvoice(clientId, today, today.plusDays(30), "1", "1000", "0");
        byte[] draftPdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", draft))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "inline; filename=\"draft-" + draft + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(textOf(draftPdf)).contains("Not yet issued");
        assertThat(withoutSpaces(textOf(draftPdf))).contains("DRAFT");

        long paid = createInvoice(clientId, today, today.plusDays(30), "1", "1000", "0");
        send(paid);
        mvc.perform(post("/api/v1/invoices/{id}/payments", paid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 1000, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().isCreated());
        assertThat(pdfText(paid)).contains("Balance due INR 0.00");
        assertThat(withoutSpaces(pdfText(paid))).contains("PAID");

        long voided = createInvoice(clientId, today, today.plusDays(30), "1", "1000", "0");
        send(voided);
        mvc.perform(post("/api/v1/invoices/{id}/void", voided)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Raised against the wrong client"}
                                """))
                .andExpect(status().isOk());
        assertThat(withoutSpaces(pdfText(voided))).contains("VOID");
    }

    @Test
    void failedRemindersAreLoggedAndSentOnTheNextRun() throws Exception {
        LocalDate today = LocalDate.now(clock);
        // The daily owner summary is not part of this test; switching it off keeps a failed
        // summary from being logged for today.
        updateSettings(false, "owner@duewise.test", "accountant@duewise.test");

        String clientEmail = "accounts+" + UUID.randomUUID() + "@example.com";
        long clientId = createClient("Tapti Logistics", clientEmail);
        long tenDaysLate = createInvoice(clientId, today.minusDays(40), today.minusDays(10), "1", "750", "0");
        String number = send(tenDaysLate);

        // Mail server down: the attempt is logged as FAILED with the error.
        greenMail.stop();
        mvc.perform(post("/api/v1/reminders/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientRemindersFailed").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        String failedLog = reminderLog();
        assertThat(logField(failedLog, tenDaysLate, "status")).isEqualTo("FAILED");
        assertThat(logField(failedLog, tenDaysLate, "attempts")).isEqualTo(1);
        assertThat(logField(failedLog, tenDaysLate, "lastError")).isNotNull();
        // Catch-up rule: 10 days overdue with no reminder yet gets the day-7 reminder.
        assertThat(logField(failedLog, tenDaysLate, "reminderDay")).isEqualTo(7);

        // Mail server back: the next run sends it, and the same log row records the second attempt.
        greenMail.start();
        mvc.perform(post("/api/v1/reminders/run")).andExpect(status().isOk());
        String sentLog = reminderLog();
        assertThat(logField(sentLog, tenDaysLate, "status")).isEqualTo("SENT");
        assertThat(logField(sentLog, tenDaysLate, "attempts")).isEqualTo(2);
        assertThat(logField(sentLog, tenDaysLate, "lastError")).isNull();
        assertThat(messagesTo(clientEmail)).extracting(MimeMessage::getSubject)
                .containsExactly("Reminder: invoice " + number + " is 10 days overdue");
    }

    @Test
    void anIssuedInvoiceIsEmailedWithItsPdfToTheBillingAddress() throws Exception {
        LocalDate today = LocalDate.now(clock);
        String billingEmail = "accounts+" + UUID.randomUUID() + "@example.com";
        long clientId = createClient("Krishna Interiors", billingEmail);
        long invoiceId = createInvoice(clientId, today, today.plusDays(30), "1", "1000", "0");
        String number = send(invoiceId);

        mvc.perform(post("/api/v1/invoices/{id}/email", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(number))
                .andExpect(jsonPath("$.sentTo").value(billingEmail))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());

        List<MimeMessage> emails = messagesTo(billingEmail);
        assertThat(emails).extracting(MimeMessage::getSubject)
                .containsExactly("Invoice " + number + " from Duewise Demo Studio");
        assertThat(hasPdfAttachment(emails.getFirst())).isTrue();
        assertThat(htmlOf(emails.getFirst())).contains("Hello Krishna Interiors").contains("due on");

        // Once paid in full, it can still be emailed: as a copy for the client's records.
        mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 1000, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().is2xxSuccessful());
        greenMail.reset();
        mvc.perform(post("/api/v1/invoices/{id}/email", invoiceId)).andExpect(status().isOk());
        assertThat(htmlOf(messagesTo(billingEmail).getFirst())).contains("paid in full");
    }

    @Test
    void draftsAndVoidInvoicesAreNotEmailed() throws Exception {
        LocalDate today = LocalDate.now(clock);
        String billingEmail = "accounts+" + UUID.randomUUID() + "@example.com";
        long clientId = createClient("Sabarmati Crafts", billingEmail);

        long draft = createInvoice(clientId, today, today.plusDays(30), "1", "500", "0");
        mvc.perform(post("/api/v1/invoices/{id}/email", draft))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_SENT"));

        long voided = createInvoice(clientId, today, today.plusDays(30), "1", "500", "0");
        send(voided);
        mvc.perform(post("/api/v1/invoices/{id}/void", voided)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Raised in error"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/invoices/{id}/email", voided))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_VOID"));

        assertThat(messagesTo(billingEmail)).isEmpty();
    }

    @Test
    void aRefusedEmailIsReportedAndNothingIsSent() throws Exception {
        LocalDate today = LocalDate.now(clock);
        String billingEmail = "accounts+" + UUID.randomUUID() + "@example.com";
        long clientId = createClient("Kosi Traders", billingEmail);
        long invoiceId = createInvoice(clientId, today, today.plusDays(30), "1", "250", "0");
        send(invoiceId);

        greenMail.stop();
        try {
            mvc.perform(post("/api/v1/invoices/{id}/email", invoiceId))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("EMAIL_FAILED"));
        } finally {
            greenMail.start();
        }
        assertThat(messagesTo(billingEmail)).isEmpty();
    }

    // ----- helpers ---------------------------------------------------------------------------

    /** The HTML part of an email, decoded (GreenMailUtil.getBody would return it still encoded). */
    private static String htmlOf(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = htmlOf(multipart.getBodyPart(i));
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }

    private String pdfText(long invoiceId) throws Exception {
        byte[] pdf = mvc.perform(get("/api/v1/invoices/{id}/pdf", invoiceId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return textOf(pdf);
    }

    /**
     * The stamps are printed with letter-spacing, which text extraction turns into real spaces
     * ("D R A F T"), so stamps are compared with all whitespace removed.
     */
    private static String withoutSpaces(String text) {
        return text.replaceAll("\\s+", "");
    }

    private String reminderLog() throws Exception {
        return mvc.perform(get("/api/v1/reminders/log").param("size", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** One field of the client-reminder log row for an invoice (exactly one row expected). */
    private static Object logField(String logJson, long invoiceId, String field) {
        List<Object> values = JsonPath.read(logJson, "$.content[?(@.invoiceId == " + invoiceId + ")]." + field);
        assertThat(values).as("log rows for invoice " + invoiceId).hasSize(1);
        return values.getFirst();
    }

    private void updateSettings(boolean ownerDigestEnabled, String owner, String cc) throws Exception {
        String current = mvc.perform(get("/api/v1/settings/reminders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int version = JsonPath.read(current, "$.version");
        mvc.perform(put("/api/v1/settings/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerDigestEnabled": %b, "clientRemindersEnabled": true,
                                 "recipients": [{"email": "%s", "type": "TO"}, {"email": "%s", "type": "CC"}],
                                 "reminderDays": [1, 7, 14], "version": %d}
                                """.formatted(ownerDigestEnabled, owner, cc, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(version + 1));
    }

    private long createClient(String name, String billingEmail) throws Exception {
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "email": "main+%s@example.com", "billingEmail": "%s",
                                 "defaultCurrencyCode": "INR", "address": {"line1": "4 Beach Road", "city": "Kakinada", "countryCode": "IN"}}
                                """.formatted(name, UUID.randomUUID(), billingEmail)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createInvoice(long clientId, LocalDate issue, LocalDate due,
                               String quantity, String unitPrice, String taxRate) throws Exception {
        String response = mvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "issueDate": "%s", "dueDate": "%s",
                                 "lines": [{"description": "Consulting", "quantity": %s, "unitPrice": %s, "taxRate": %s}]}
                                """.formatted(clientId, issue, due, quantity, unitPrice, taxRate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String send(long invoiceId) throws Exception {
        String response = mvc.perform(post("/api/v1/invoices/{id}/send", invoiceId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.invoiceNumber");
    }

    /**
     * Distinct emails sent TO one address (other tests' overdue invoices get reminders too).
     * GreenMail stores a copy in every recipient's mailbox, so an email to the owner with the
     * accountant on CC is received twice; copies share a Message-ID and are counted once.
     */
    private static List<MimeMessage> messagesTo(String address) {
        Map<String, MimeMessage> unique = new LinkedHashMap<>();
        for (MimeMessage message : greenMail.getReceivedMessages()) {
            try {
                Address[] to = message.getRecipients(Message.RecipientType.TO);
                boolean addressedTo = to != null && Arrays.stream(to)
                        .anyMatch(recipient -> recipient.toString().equalsIgnoreCase(address));
                if (addressedTo) {
                    String id = message.getMessageID();
                    unique.putIfAbsent(id != null ? id : "no-id-" + System.identityHashCode(message), message);
                }
            } catch (MessagingException e) {
                throw new IllegalStateException(e);
            }
        }
        return List.copyOf(unique.values());
    }

    private static boolean hasPdfAttachment(Part part) throws Exception {
        if (part.isMimeType("application/pdf")) {
            return true;
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                if (hasPdfAttachment(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String textOf(byte[] pdf) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }
}
