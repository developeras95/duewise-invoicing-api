package com.duewise.currency;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duewise.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer test: only the controller, JSON handling, validation and the error handler are
 * loaded. The service is a mock, so no database is needed.
 */
@WebMvcTest(CurrencyController.class)
class CurrencyControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CurrencyService service;

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(service.create(any())).thenReturn(new CurrencyResponse("CHF", "Swiss Franc", "CHF", 2, true));

        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "CHF", "name": "Swiss Franc", "symbol": "CHF", "minorUnits": 2}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/currencies/CHF")))
                .andExpect(jsonPath("$.code").value("CHF"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void invalidBodyReturns400WithOneErrorPerField() throws Exception {
        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "chf", "name": "", "minorUnits": 5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors", hasSize(4)))
                .andExpect(jsonPath("$.errors[*].field", hasItems("code", "name", "symbol", "minorUnits")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unknownCurrencyReturns404Problem() throws Exception {
        when(service.get("XYZ")).thenThrow(new NotFoundException("CURRENCY_NOT_FOUND", "Currency XYZ was not found."));

        mvc.perform(get("/api/v1/currencies/XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CURRENCY_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Currency XYZ was not found."));
    }

    @Test
    void blankNameInPatchIsRejected() throws Exception {
        mvc.perform(patch("/api/v1/currencies/INR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }
}
