package com.duewise.invoice;

import java.net.URI;
import java.time.LocalDate;

import com.duewise.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Draft, send and void invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List invoices", description = "Paginated, with optional filters. Combine them freely, e.g. overdue invoices for one client.")
    public PageResponse<InvoiceSummaryResponse> list(
            @Parameter(description = "Only this status")
            @RequestParam(required = false) InvoiceStatus status,
            @Parameter(description = "Only this client's invoices")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "true = only invoices with money owed past their due date")
            @RequestParam(defaultValue = "false") boolean overdue,
            @Parameter(description = "Issued on or after this date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedFrom,
            @Parameter(description = "Issued on or before this date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "issueDate, dueDate, total, invoiceNumber or createdAt")
            @RequestParam(defaultValue = "issueDate")
            @Pattern(regexp = "issueDate|dueDate|total|invoiceNumber|createdAt",
                    message = "must be issueDate, dueDate, total, invoiceNumber or createdAt") String sort,
            @RequestParam(defaultValue = "desc") @Pattern(regexp = "asc|desc", message = "must be asc or desc") String direction) {

        InvoiceFilter filter = new InvoiceFilter(status, clientId, overdue, issuedFrom, issuedTo);
        Sort order = Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by(Sort.Direction.DESC, "id"));
        return service.list(filter, PageRequest.of(page, size, order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one invoice with its line items")
    public InvoiceResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a draft invoice",
            description = "Currency, issue date and due date default from the client and today. Totals are calculated for you.")
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody InvoiceRequest request) {
        InvoiceResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a draft",
            description = "Only drafts can be changed. The lines sent replace all existing lines. Requires the current 'version'.")
    public InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a draft", description = "Sent invoices cannot be deleted; void them instead.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Send (issue) a draft",
            description = "Assigns the next invoice number for the issue year (no gaps) and locks the invoice against edits.")
    public InvoiceResponse send(@PathVariable Long id) {
        return service.send(id);
    }

    @PostMapping("/{id}/void")
    @Operation(summary = "Void a sent invoice", description = "Only possible while no payment has been recorded. A reason is required.")
    public InvoiceResponse voidInvoice(@PathVariable Long id, @Valid @RequestBody VoidInvoiceRequest request) {
        return service.voidInvoice(id, request);
    }
}
