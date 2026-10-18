package com.duewise.currency;

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
@RequestMapping("/api/v1/currencies")
@Tag(name = "Currencies", description = "Currencies that invoices can be raised in")
public class CurrencyController {

    private final CurrencyService service;

    public CurrencyController(CurrencyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List currencies", description = "Sorted by code. Leave 'active' empty to get all of them.")
    public List<CurrencyResponse> list(
            @Parameter(description = "true = only active, false = only inactive")
            @RequestParam(required = false) Boolean active) {
        return service.list(active);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get one currency by ISO code")
    public CurrencyResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @PostMapping
    @Operation(summary = "Add a currency", description = "Returns 409 if the code already exists.")
    public ResponseEntity<CurrencyResponse> create(@Valid @RequestBody CreateCurrencyRequest request) {
        CurrencyResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(created.code())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{code}")
    @Operation(summary = "Update a currency",
            description = "Change the name or symbol, or deactivate it. Only the fields you send are changed.")
    public CurrencyResponse update(@PathVariable String code, @Valid @RequestBody UpdateCurrencyRequest request) {
        return service.update(code, request);
    }
}
