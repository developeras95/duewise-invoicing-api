package com.duewise.statement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Statements", description = "What a client owes and how late it is")
public class StatementController {

    private final StatementService service;

    public StatementController(StatementService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/clients/{clientId}/statement")
    @Operation(summary = "Get a client's statement",
            description = "Every SENT or PARTIALLY_PAID invoice with its balance, and totals per currency split into "
                    + "current, 1-30, 31-60, 61-90 and over 90 days past due. Drafts, paid and void invoices are left out.")
    public StatementResponse get(@PathVariable Long clientId) {
        return service.forClient(clientId);
    }
}
