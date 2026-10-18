package com.duewise.paymentmethod;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/payment-methods")
@Tag(name = "Payment methods", description = "Ways clients can pay an invoice")
public class PaymentMethodController {

    private final PaymentMethodService service;

    public PaymentMethodController(PaymentMethodService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List payment methods", description = "Sorted by display order. Leave 'active' empty to get all of them.")
    public List<PaymentMethodResponse> list(
            @Parameter(description = "true = only active, false = only inactive")
            @RequestParam(required = false) Boolean active) {
        return service.list(active);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get one payment method by code")
    public PaymentMethodResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @PostMapping
    @Operation(summary = "Add a payment method", description = "Returns 409 if the code already exists.")
    public ResponseEntity<PaymentMethodResponse> create(@Valid @RequestBody CreatePaymentMethodRequest request) {
        PaymentMethodResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(created.code())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{code}")
    @Operation(summary = "Update a payment method",
            description = "Rename it, change its order, or deactivate it. Only the fields you send are changed.")
    public PaymentMethodResponse update(
            @PathVariable String code, @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return service.update(code, request);
    }
}
