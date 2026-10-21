package com.duewise;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** End-to-end payments and statements against a real PostgreSQL. */
@AutoConfigureMockMvc
class PaymentAndStatementApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    @Test
    void paymentsTakeAnInvoiceFromSentToPaid() throws Exception {
        long clientId = createClient();
        LocalDate today = LocalDate.now(clock);
        long invoiceId = createInvoice(clientId, today, today.plusDays(30), "2", "12500.50", "18");   // 29501.18
        send(invoiceId);

        pay(invoiceId, """
                {"amount": 10000, "paymentMethod": "UPI"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFERENCE_REQUIRED"));

        pay(invoiceId, """
                {"amount": 10000, "paymentMethod": "UPI", "reference": "UPI-4521987730"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payment.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.payment.paidOn").value(today.toString()))
                .andExpect(jsonPath("$.invoice.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.invoice.balanceDue").value(19501.18));

        pay(invoiceId, """
                {"amount": 20000, "paymentMethod": "CASH"}
                """)
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("OVERPAYMENT"));

        pay(invoiceId, """
                {"amount": 19501.18, "paymentMethod": "BANK_TRANSFER", "reference": "NEFT-88231"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.status").value("PAID"))
                .andExpect(jsonPath("$.invoice.balanceDue").value(0.0));

        pay(invoiceId, """
                {"amount": 1, "paymentMethod": "CASH"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_ALREADY_PAID"));

        mvc.perform(get("/api/v1/invoices/{id}/payments", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].paymentMethod").value("UPI"))
                .andExpect(jsonPath("$[1].amount").value(19501.18));

        mvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt", notNullValue()))
                .andExpect(jsonPath("$.amountPaid").value(29501.18));

        mvc.perform(post("/api/v1/invoices/{id}/void", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Too late"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_HAS_PAYMENTS"));
    }

    @Test
    void draftsTakeNoPaymentsAndUnknownInvoicesAre404() throws Exception {
        long clientId = createClient();
        LocalDate today = LocalDate.now(clock);
        long draftId = createInvoice(clientId, today, today.plusDays(30), "1", "1000", "0");

        pay(draftId, """
                {"amount": 100, "paymentMethod": "CASH"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_SENT"));

        pay(999999L, """
                {"amount": 100, "paymentMethod": "CASH"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"));
    }

    @Test
    void statementAgesEveryOpenBalance() throws Exception {
        long clientId = createClient();
        LocalDate today = LocalDate.now(clock);

        long tenDaysLate = createInvoice(clientId, today.minusDays(40), today.minusDays(10), "1", "1000", "0");
        long fortyFiveDaysLate = createInvoice(clientId, today.minusDays(75), today.minusDays(45), "1", "2000", "0");
        long notYetDue = createInvoice(clientId, today, today.plusDays(5), "1", "3000", "0");
        createInvoice(clientId, today, today.plusDays(5), "1", "9000", "0");     // stays a draft: left out
        send(tenDaysLate);
        send(fortyFiveDaysLate);
        send(notYetDue);
        pay(fortyFiveDaysLate, """
                {"amount": 500, "paymentMethod": "CASH"}
                """).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/clients/{id}/statement", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").value(today.toString()))
                .andExpect(jsonPath("$.balances", hasSize(1)))
                .andExpect(jsonPath("$.balances[0].currencyCode").value("INR"))
                .andExpect(jsonPath("$.balances[0].openInvoices").value(3))
                .andExpect(jsonPath("$.balances[0].totalOutstanding").value(5500.0))
                .andExpect(jsonPath("$.balances[0].current").value(3000.0))
                .andExpect(jsonPath("$.balances[0].days1To30").value(1000.0))
                .andExpect(jsonPath("$.balances[0].days31To60").value(1500.0))
                .andExpect(jsonPath("$.balances[0].days61To90").value(0.0))
                .andExpect(jsonPath("$.balances[0].over90").value(0.0))
                .andExpect(jsonPath("$.openInvoices", hasSize(3)))
                .andExpect(jsonPath("$.openInvoices[0].id").value((int) fortyFiveDaysLate))
                .andExpect(jsonPath("$.openInvoices[0].daysOverdue").value(45))
                .andExpect(jsonPath("$.openInvoices[0].agingBucket").value("DAYS_31_60"))
                .andExpect(jsonPath("$.openInvoices[2].agingBucket").value("CURRENT"));
    }

    @Test
    void statementForUnknownClientIs404() throws Exception {
        mvc.perform(get("/api/v1/clients/999999/statement"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    // ----- helpers ---------------------------------------------------------------------------

    private long createClient() throws Exception {
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Payment Test Client", "email": "pay+%s@example.com", "defaultCurrencyCode": "INR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private long createInvoice(long clientId, LocalDate issue, LocalDate due,
                               String quantity, String unitPrice, String taxRate) throws Exception {
        String response = mvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "issueDate": "%s", "dueDate": "%s",
                                 "lines": [{"description": "Work", "quantity": %s, "unitPrice": %s, "taxRate": %s}]}
                                """.formatted(clientId, issue, due, quantity, unitPrice, taxRate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private void send(long invoiceId) throws Exception {
        mvc.perform(post("/api/v1/invoices/{id}/send", invoiceId)).andExpect(status().isOk());
    }

    private ResultActions pay(long invoiceId, String body) throws Exception {
        return mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
