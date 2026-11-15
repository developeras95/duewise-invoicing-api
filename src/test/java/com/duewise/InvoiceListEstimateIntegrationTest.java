package com.duewise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The unfiltered invoice list with an estimated total. The threshold is set to 0 here so that a test
 * database of a few invoices behaves like one of millions.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "duewise.invoices.estimate-total-above=0")
class InvoiceListEstimateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void theUnfilteredListTakesPostgresEstimateAndFilteredListsCount() throws Exception {
        long clientId = createClient();
        createDraft(clientId);
        createDraft(clientId);
        jdbc.execute("ANALYZE invoice");   // what autovacuum does on a real database
        long invoices = jdbc.queryForObject("SELECT count(*) FROM invoice", Long.class);

        String page = mvc.perform(get("/api/v1/invoices?size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExact").value(false))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        long total = ((Number) JsonPath.read(page, "$.totalElements")).longValue();
        assertThat(total).isEqualTo(invoices);
        assertThat(((Number) JsonPath.read(page, "$.totalPages")).longValue()).isEqualTo(invoices);

        mvc.perform(get("/api/v1/invoices?clientId={id}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExact").value(true))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private long createClient() throws Exception {
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Estimate Test Client", "email": "estimate+%s@example.com", "defaultCurrencyCode": "INR"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void createDraft(long clientId) throws Exception {
        mvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": %d, "lines": [{"description": "Work", "quantity": 1, "unitPrice": 100}]}
                                """.formatted(clientId)))
                .andExpect(status().isCreated());
    }
}
