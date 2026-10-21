package com.duewise.payment;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/payments")
@Tag(name = "Payments", description = "Money received against an invoice")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Record a payment",
            description = "Only for SENT or PARTIALLY_PAID invoices. Moves the invoice to PARTIALLY_PAID, "
                    + "or PAID when the balance reaches zero. Overpayments are refused (422).")
    public ResponseEntity<PaymentReceipt> record(@PathVariable Long invoiceId, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.record(invoiceId, request));
    }

    @GetMapping
    @Operation(summary = "List an invoice's payments", description = "Oldest first.")
    public List<PaymentResponse> list(@PathVariable Long invoiceId) {
        return service.list(invoiceId);
    }
}
