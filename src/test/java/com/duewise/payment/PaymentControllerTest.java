package com.duewise.payment;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.duewise.common.error.BusinessRuleException;
import com.duewise.invoice.InvoiceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PaymentService service;

    @Test
    void zeroAmountAndMissingMethodAreReported() throws Exception {
        mvc.perform(post("/api/v1/invoices/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("amount", "paymentMethod")));
    }

    @Test
    void recordedPaymentReturns201WithTheNewBalance() throws Exception {
        PaymentReceipt receipt = new PaymentReceipt(
                new PaymentResponse(9L, "CASH", "Cash", new BigDecimal("400.00"), LocalDate.of(2026, 10, 21), null, null, null),
                new PaymentReceipt.InvoiceBalance(1L, "INV-2026-00001", InvoiceStatus.PARTIALLY_PAID, "INR",
                        new BigDecimal("1000.00"), new BigDecimal("400.00"), new BigDecimal("600.00"), 3));
        when(service.record(eq(1L), any())).thenReturn(receipt);

        mvc.perform(post("/api/v1/invoices/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 400, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoice.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.invoice.balanceDue").value(600.00));
    }

    @Test
    void overpaymentIsA422() throws Exception {
        when(service.record(eq(1L), any())).thenThrow(new BusinessRuleException("OVERPAYMENT",
                "A payment of 2000.00 INR is more than the balance due of 600.00 INR."));

        mvc.perform(post("/api/v1/invoices/1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 2000, "paymentMethod": "CASH"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("OVERPAYMENT"));
    }
}
