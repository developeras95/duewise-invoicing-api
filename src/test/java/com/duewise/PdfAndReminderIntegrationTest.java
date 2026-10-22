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
        updateSettings("owner@duewise.test", "accountant@duewise.test");

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

    // ----- helpers ---------------------------------------------------------------------------

    private void updateSettings(String owner, String cc) throws Exception {
        String current = mvc.perform(get("/api/v1/settings/reminders"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int version = JsonPath.read(current, "$.version");
        mvc.perform(put("/api/v1/settings/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerDigestEnabled": true, "clientRemindersEnabled": true,
                                 "recipients": [{"email": "%s", "type": "TO"}, {"email": "%s", "type": "CC"}],
                                 "reminderDays": [1, 7, 14], "version": %d}
                                """.formatted(owner, cc, version)))
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
