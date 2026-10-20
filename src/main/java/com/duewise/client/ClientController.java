package com.duewise.client;

import java.net.URI;

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
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/clients")
@Tag(name = "Clients", description = "Customers that invoices are raised for")
public class ClientController {

    private final ClientService service;

    public ClientController(ClientService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List or search clients", description = "Paginated. 'q' matches any part of the name or email, ignoring case.")
    public PageResponse<ClientResponse> list(
            @Parameter(description = "Text to find in name or email", example = "acme")
            @RequestParam(required = false) String q,
            @Parameter(description = "Zero-based page number")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, 1 to 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field: name, email or createdAt")
            @RequestParam(defaultValue = "name") @Pattern(regexp = "name|email|createdAt", message = "must be name, email or createdAt") String sort,
            @Parameter(description = "asc or desc")
            @RequestParam(defaultValue = "asc") @Pattern(regexp = "asc|desc", message = "must be asc or desc") String direction) {

        Sort order = Sort.by(Sort.Direction.fromString(direction), sort).and(Sort.by("id"));
        return service.search(q, PageRequest.of(page, size, order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one client")
    public ClientResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a client", description = "Returns 409 if another client already uses the email (ignoring case).")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a client",
            description = "Send every field plus the 'version' from your last GET. Optional fields you leave out are cleared. "
                    + "Returns 409 if the client was changed by someone else in the meantime.")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return service.update(id, request);
    }
}
