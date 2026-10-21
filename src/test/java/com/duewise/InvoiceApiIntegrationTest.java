package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end invoice tests against a real PostgreSQL: lifecycle, numbering and filters. */
@AutoConfigureMockMvc
class InvoiceApiIntegrationTest extends AbstractIntegrationTest {

    private static final String TWO_LINES = """
            [{"description": "Brand identity", "quantity": 2, "unitPrice": 12500.50, "taxRate": 18},
             {"description": "Consulting hours", "quantity": 1.5, "unitPrice": 999.99}]
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    @Test
    void draftCanBeEditedThenSentThenVoided() throws Exception {
        long clientId = createClient("INR", 15);
        LocalDate today = LocalDate.now(clock);

        // Create: currency and due date come from the client, totals are calculated.
        String created = createDraft("""
                {"clientId": %d, "lines": %s}
                """.formatted(clientId, TWO_LINES))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.invoiceNumber").value(nullValue()))
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.dueDate").value(today.plusDays(15).toString()))
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[1].lineSubtotal").value(1499.99))
                .andExpect(jsonPath("$.subtotal").value(26500.99))
                .andExpect(jsonPath("$.taxTotal").value(4500.18))
                .andExpect(jsonPath("$.total").value(31001.17))
                .andReturn().getResponse().getContentAsString();
        long id = idOf(created);

        // Edit the draft: three lines become one.
        mvc.perform(put("/api/v1/invoices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "version": 0,
                                 "lines": [{"description": "Brand identity", "quantity": 2, "unitPrice": 12500.50, "taxRate": 18}]}
                                """.formatted(clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.total").value(29501.18))
                .andExpect(jsonPath("$.version").value(1));

        // Send: gets a number and can no longer be edited or deleted.
        mvc.perform(post("/api/v1/invoices/{id}/send", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.invoiceNumber", matchesPattern("INV-\\d{4}-\\d{5}")));

        String sentJson = mvc.perform(get("/api/v1/invoices/{id}", id)).andReturn().getResponse().getContentAsString();
        int currentVersion = JsonPath.read(sentJson, "$.version");
        mvc.perform(put("/api/v1/invoices/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "version": %d, "lines": []}
                                """.formatted(clientId, currentVersion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_EDITABLE"));

        mvc.perform(delete("/api/v1/invoices/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_DELETABLE"));

        // Void: allowed once, with a reason.
        mvc.perform(post("/api/v1/invoices/{id}/void", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Raised against the wrong client"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOID"))
                .andExpect(jsonPath("$.voidReason").value("Raised against the wrong client"));

        mvc.perform(post("/api/v1/invoices/{id}/void", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Again"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void numbersAreConsecutiveEvenWhenADraftIsDeleted() throws Exception {
        long clientId = createClient("INR", 30);

        int first = sequenceOf(send(createDraftId(clientId)));

        long abandoned = createDraftId(clientId);
        mvc.perform(delete("/api/v1/invoices/{id}", abandoned)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/invoices/{id}", abandoned)).andExpect(status().isNotFound());

        int second = sequenceOf(send(createDraftId(clientId)));

        assertThat(second).isEqualTo(first + 1);
    }

    @Test
    void overdueFilterReturnsOnlySentInvoicesPastTheirDueDate() throws Exception {
        long clientId = createClient("INR", 30);
        LocalDate today = LocalDate.now(clock);
        String pastDates = """
                {"clientId": %d, "issueDate": "%s", "dueDate": "%s", "lines": %s}
                """.formatted(clientId, today.minusDays(40), today.minusDays(10), TWO_LINES);

        long overdueId = idOf(createDraft(pastDates).andReturn().getResponse().getContentAsString());
        send(overdueId);
        long draftId = idOf(createDraft(pastDates).andReturn().getResponse().getContentAsString());
        long notYetDueId = createDraftId(clientId);
        send(notYetDueId);

        mvc.perform(get("/api/v1/invoices").param("clientId", String.valueOf(clientId)).param("overdue", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) overdueId))
                .andExpect(jsonPath("$.content[0].overdue").value(true))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) draftId))))
                .andExpect(jsonPath("$.content[*].id", not(hasItem((int) notYetDueId))));
    }

    @Test
    void yenPricesCannotHaveDecimals() throws Exception {
        long clientId = createClient(null, 30);

        createDraft("""
                {"clientId": %d, "currencyCode": "JPY",
                 "lines": [{"description": "Translation", "quantity": 1, "unitPrice": 1500.50}]}
                """.formatted(clientId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AMOUNT_PRECISION"));
    }

    @Test
    void emptyDraftCannotBeSent() throws Exception {
        long clientId = createClient("INR", 30);
        long id = idOf(createDraft("""
                {"clientId": %d}
                """.formatted(clientId)).andReturn().getResponse().getContentAsString());

        mvc.perform(post("/api/v1/invoices/{id}/send", id))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INVOICE_HAS_NO_LINES"));
    }

    @Test
    void unknownClientIsABusinessRuleError() throws Exception {
        createDraft("""
                {"clientId": 999999, "currencyCode": "INR"}
                """)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    // ----- helpers ---------------------------------------------------------------------------

    private long createClient(String currency, int paymentTermsDays) throws Exception {
        String email = "billing+" + UUID.randomUUID() + "@example.com";
        String currencyJson = currency == null ? "null" : "\"" + currency + "\"";
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invoice Test Client", "email": "%s", "defaultCurrencyCode": %s, "paymentTermsDays": %d}
                                """.formatted(email, currencyJson, paymentTermsDays)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private ResultActions createDraft(String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createDraftId(long clientId) throws Exception {
        String response = createDraft("""
                {"clientId": %d, "lines": %s}
                """.formatted(clientId, TWO_LINES))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    /** Sends the invoice and returns its number. */
    private String send(long id) throws Exception {
        String response = mvc.perform(post("/api/v1/invoices/{id}/send", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.invoiceNumber");
    }

    /** INV-2026-00042 → 42 */
    private static int sequenceOf(String invoiceNumber) {
        return Integer.parseInt(invoiceNumber.substring(invoiceNumber.lastIndexOf('-') + 1));
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
