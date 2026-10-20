package com.duewise;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end client tests against a real PostgreSQL. Each test uses its own email addresses so
 * the tests do not depend on each other's data.
 */
@AutoConfigureMockMvc
class ClientApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createdClientCanBeFetchedWithItsAddressAndCurrency() throws Exception {
        String location = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nilgiri Tea Traders", "email": "orders@nilgiritea.in",
                                 "defaultCurrencyCode": "inr", "paymentTermsDays": 15,
                                 "address": {"city": "Coonoor", "countryCode": "IN"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nilgiri Tea Traders"))
                .andExpect(jsonPath("$.defaultCurrencyCode").value("INR"))
                .andExpect(jsonPath("$.paymentTermsDays").value(15))
                .andExpect(jsonPath("$.address.city").value("Coonoor"));
    }

    @Test
    void emailIsUniqueIgnoringCase() throws Exception {
        createClient("Kaveri Exports", "hello@kaveri.in");

        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kaveri Exports (duplicate)", "email": "Hello@Kaveri.IN"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_EMAIL_TAKEN"));
    }

    @Test
    void unknownCurrencyIsABusinessRuleError() throws Exception {
        mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Mystery Co", "email": "info@mystery.example", "defaultCurrencyCode": "XYZ"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("CURRENCY_NOT_FOUND"));
    }

    @Test
    void searchMatchesPartOfNameOrEmailIgnoringCase() throws Exception {
        createClient("Brahmaputra Logistics", "ops@brahmaputra-logistics.in");
        createClient("Godavari Foods", "accounts@godavarifoods.in");

        mvc.perform(get("/api/v1/clients").param("q", "BRAHMA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Brahmaputra Logistics")))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Godavari Foods"))));

        mvc.perform(get("/api/v1/clients").param("q", "godavarifoods.in"))
                .andExpect(jsonPath("$.content[*].name", hasItem("Godavari Foods")));
    }

    @Test
    void updateWithStaleVersionIsRefused() throws Exception {
        String location = createClient("Narmada Textiles", "sales@narmadatex.in");

        // First edit, from version 0: succeeds and moves the client to version 1.
        mvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Narmada Textiles Pvt Ltd", "email": "sales@narmadatex.in", "version": 0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Narmada Textiles Pvt Ltd"))
                .andExpect(jsonPath("$.version").value(1));

        // Second edit still based on version 0: someone else's change would be lost, so it is refused.
        mvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Narmada Tex", "email": "sales@narmadatex.in", "version": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
    }

    @Test
    void unknownClientReturns404() throws Exception {
        mvc.perform(get("/api/v1/clients/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    private String createClient(String name, String email) throws Exception {
        String body = """
                {"name": "%s", "email": "%s"}
                """.formatted(name, email);
        String response = mvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(response, "$.id");
        return "/api/v1/clients/" + id;
    }
}
