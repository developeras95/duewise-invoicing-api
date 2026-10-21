package com.duewise.invoice;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duewise.common.error.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    InvoiceService service;

    @Test
    void invalidLineItemsAreReportedByPosition() throws Exception {
        mvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines": [
                                   {"description": "Design", "quantity": 0, "unitPrice": -5},
                                   {"description": "", "quantity": 1, "unitPrice": 10, "taxRate": 150}
                                ]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItems(
                        "clientId", "lines[0].quantity", "lines[0].unitPrice",
                        "lines[1].description", "lines[1].taxRate")));
    }

    @Test
    void unknownStatusFilterIsA400() throws Exception {
        mvc.perform(get("/api/v1/invoices").param("status", "LOST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusedSendIsA409() throws Exception {
        when(service.send(3L)).thenThrow(
                new ConflictException("INVALID_STATUS_TRANSITION", "Invoice INV-2026-00001 is already SENT."));

        mvc.perform(post("/api/v1/invoices/3/send"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void voidRequiresAReason() throws Exception {
        mvc.perform(post("/api/v1/invoices/3/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void deletingADraftReturns204() throws Exception {
        mvc.perform(delete("/api/v1/invoices/3"))
                .andExpect(status().isNoContent());
    }
}
