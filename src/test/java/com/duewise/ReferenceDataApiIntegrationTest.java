package com.duewise;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end tests for currencies and payment methods: HTTP request, service, cache and a real
 * PostgreSQL. Each test uses its own codes so tests do not depend on each other's data.
 */
@AutoConfigureMockMvc
class ReferenceDataApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> Objects.requireNonNull(cacheManager.getCache(name)).clear());
    }

    @Test
    void seededCurrenciesAreListedWithTheirDecimalPlaces() throws Exception {
        mvc.perform(get("/api/v1/currencies").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'JPY')].minorUnits").value(0))
                .andExpect(jsonPath("$[?(@.code == 'KWD')].minorUnits").value(3));
    }

    @Test
    void addingAnExistingCurrencyReturns409() throws Exception {
        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "INR", "name": "Indian Rupee", "symbol": "₹", "minorUnits": 2}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CURRENCY_ALREADY_EXISTS"));
    }

    @Test
    void newCurrencyCanBeCreatedAndFetched() throws Exception {
        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CHF", "name": "Swiss Franc", "symbol": "CHF", "minorUnits": 2}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/currencies/chf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Swiss Franc"));
    }

    /**
     * Proves the cache is cleared on change: the active list is read (and cached) before the
     * method is deactivated, and must not contain it afterwards.
     */
    @Test
    void deactivatedPaymentMethodDisappearsFromTheActiveList() throws Exception {
        mvc.perform(post("/api/v1/payment-methods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "TEST_WALLET", "displayName": "Test wallet", "requiresReference": true, "sortOrder": 99}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/payment-methods").param("active", "true"))
                .andExpect(jsonPath("$[*].code", hasItem("TEST_WALLET")));

        mvc.perform(patch("/api/v1/payment-methods/TEST_WALLET")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(get("/api/v1/payment-methods").param("active", "true"))
                .andExpect(jsonPath("$[*].code", not(hasItem("TEST_WALLET"))));
    }

    @Test
    void unknownPaymentMethodReturns404() throws Exception {
        mvc.perform(get("/api/v1/payment-methods/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PAYMENT_METHOD_NOT_FOUND"));
    }
}
