package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The revenue report against a real PostgreSQL: finished months from the stored report, the current month
 * live. Its payments are in a currency only this test uses, so other tests' payments cannot change the sums.
 */
@AutoConfigureMockMvc
class RevenueReportIntegrationTest extends AbstractIntegrationTest {

    private static final String CURRENCY = "XRV";

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    @Test
    void theCurrentMonthIsLiveAndFinishedMonthsAppearAfterARefresh() throws Exception {
        LocalDate today = LocalDate.now(clock);
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        createCurrency();
        long clientId = createClient();

        long lastMonthsInvoice = createInvoice(clientId, lastMonth.atDay(1), "1000");
        send(lastMonthsInvoice);
        pay(lastMonthsInvoice, "1000", lastMonth.atDay(1));

        long thisMonthsInvoice = createInvoice(clientId, today, "500");
        send(thisMonthsInvoice);
        pay(thisMonthsInvoice, "250", today);
        pay(thisMonthsInvoice, "100", today);

        // Before a refresh: this month's payments are counted live; last month's are not stored yet.
        String report = report("?from=" + lastMonth + "&to=" + thisMonth);
        assertThat(JsonPath.<String>read(report, "$.liveMonth")).isEqualTo(thisMonth.toString());
        assertThat(JsonPath.<String>read(report, "$.storedAsOf")).isNotBlank();
        assertThat(received(report, thisMonth)).containsExactly(350.0);
        assertThat(paymentCount(report, thisMonth)).containsExactly(2);
        assertThat(received(report, lastMonth)).isEmpty();
        assertThat(totalReceived(report)).containsExactly(350.0);

        mvc.perform(post("/api/v1/reports/revenue/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshedAt").isNotEmpty())
                .andExpect(jsonPath("$.tookMs").isNumber());

        report = report("?from=" + lastMonth + "&to=" + thisMonth);
        assertThat(received(report, lastMonth)).containsExactly(1000.0);
        assertThat(received(report, thisMonth)).containsExactly(350.0);
        assertThat(totalReceived(report)).containsExactly(1350.0);
        assertThat(JsonPath.<List<Integer>>read(report, "$.totals[?(@.currencyCode == '" + CURRENCY + "')].payments"))
                .containsExactly(3);

        // A period that has ended is read only from the stored report.
        report = report("?from=" + lastMonth + "&to=" + lastMonth);
        assertThat(JsonPath.<Object>read(report, "$.liveMonth")).isNull();
        assertThat(received(report, lastMonth)).containsExactly(1000.0);
        assertThat(received(report, thisMonth)).isEmpty();
    }

    @Test
    void withoutMonthsTheReportCoversTheLastTwelve() throws Exception {
        YearMonth thisMonth = YearMonth.now(clock);

        mvc.perform(get("/api/v1/reports/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(thisMonth.minusMonths(11).toString()))
                .andExpect(jsonPath("$.to").value(thisMonth.toString()))
                .andExpect(jsonPath("$.liveMonth").value(thisMonth.toString()));
    }

    @Test
    void invalidPeriodsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/reports/revenue?from=2026-10&to=2026-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MONTH_RANGE"));
        mvc.perform(get("/api/v1/reports/revenue?from=2010-01&to=2026-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MONTH_RANGE_TOO_LONG"));
        mvc.perform(get("/api/v1/reports/revenue?from=2026-13"))
                .andExpect(status().isBadRequest());
    }

    // ----- helpers ---------------------------------------------------------------------------

    private String report(String query) throws Exception {
        return mvc.perform(get("/api/v1/reports/revenue" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<Double> received(String report, YearMonth month) {
        List<Number> values = JsonPath.read(report,
                "$.months[?(@.currencyCode == '" + CURRENCY + "' && @.month == '" + month + "')].received");
        return values.stream().map(Number::doubleValue).toList();
    }

    private static List<Integer> paymentCount(String report, YearMonth month) {
        return JsonPath.read(report,
                "$.months[?(@.currencyCode == '" + CURRENCY + "' && @.month == '" + month + "')].payments");
    }

    private static List<Double> totalReceived(String report) {
        List<Number> values = JsonPath.read(report, "$.totals[?(@.currencyCode == '" + CURRENCY + "')].received");
        return values.stream().map(Number::doubleValue).toList();
    }

    private void createCurrency() throws Exception {
        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s", "name": "Revenue test unit", "symbol": "R", "minorUnits": 2}
                                """.formatted(CURRENCY)))
                .andExpect(status().isCreated());
    }

    private long createClient() throws Exception {
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Revenue Test Client", "email": "revenue+%s@example.com", "defaultCurrencyCode": "%s"}
                                """.formatted(UUID.randomUUID(), CURRENCY)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private long createInvoice(long clientId, LocalDate issue, String amount) throws Exception {
        String response = mvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "issueDate": "%s", "dueDate": "%s",
                                 "lines": [{"description": "Work", "quantity": 1, "unitPrice": %s, "taxRate": 0}]}
                                """.formatted(clientId, issue, issue.plusDays(30), amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    private void send(long invoiceId) throws Exception {
        mvc.perform(post("/api/v1/invoices/{id}/send", invoiceId)).andExpect(status().isOk());
    }

    private void pay(long invoiceId, String amount, LocalDate paidOn) throws Exception {
        mvc.perform(post("/api/v1/invoices/{id}/payments", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": %s, "paymentMethod": "CASH", "paidOn": "%s"}
                                """.formatted(amount, paidOn)))
                .andExpect(status().isCreated());
    }

    private static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
