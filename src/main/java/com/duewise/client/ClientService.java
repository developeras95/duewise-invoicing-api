package com.duewise.client;

import java.util.Locale;
import java.util.Objects;

import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.BusinessRuleException;
import com.duewise.common.error.ConflictException;
import com.duewise.common.error.NotFoundException;
import com.duewise.common.web.PageResponse;
import com.duewise.currency.Currency;
import com.duewise.currency.CurrencyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClientService {

    static final short DEFAULT_PAYMENT_TERMS_DAYS = 30;

    private final ClientRepository repository;
    private final CurrencyService currencyService;

    public ClientService(ClientRepository repository, CurrencyService currencyService) {
        this.repository = repository;
        this.currencyService = currencyService;
    }

    /** Lists clients, optionally filtered by text that appears in the name or email (any case). */
    public PageResponse<ClientResponse> search(String query, Pageable pageable) {
        String text = trimToNull(query);
        Page<Client> page = text == null
                ? repository.findAll(pageable)
                : repository.search(likePattern(text), pageable);
        return PageResponse.from(page, ClientResponse::from);
    }

    public ClientResponse get(Long id) {
        return ClientResponse.from(find(id));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCase(email)) {
            throw emailTaken(email);
        }
        Currency currency = resolveCurrency(request.defaultCurrencyCode(), null);
        Client client = new Client(toDetails(request, email, currency));
        return ClientResponse.from(repository.save(client));
    }

    /**
     * Replaces all editable fields. The caller must send the version it last read; if someone else
     * has changed the client since, the update is refused with 409 instead of overwriting their work.
     */
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        if (request.version() == null) {
            throw new BadRequestException("VERSION_REQUIRED",
                    "Send the client's current 'version' when updating, as returned by GET.");
        }
        Client client = find(id);
        if (client.getVersion() != request.version()) {
            throw new ConflictException("CONCURRENT_MODIFICATION",
                    "Client " + id + " was changed by someone else (now at version " + client.getVersion()
                            + "). Reload it and try again.");
        }
        String email = request.email().trim();
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailTaken(email);
        }
        Currency currency = resolveCurrency(request.defaultCurrencyCode(), client.getDefaultCurrency());
        client.apply(toDetails(request, email, currency));
        // Flush now so the response carries the new version and updatedAt.
        return ClientResponse.from(repository.saveAndFlush(client));
    }

    /**
     * Used by invoices to load the client named in their request body. A bad id there is a
     * business-rule error (422), not a missing URL resource (404).
     */
    public Client require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CLIENT_NOT_FOUND", "Client " + id + " does not exist."));
    }

    private Client find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " was not found."));
    }

    /**
     * New references must point at an active currency. A client that already uses a currency which
     * has since been deactivated may keep it, so unrelated edits are not blocked.
     */
    private Currency resolveCurrency(String code, Currency current) {
        String normalized = trimToNull(code);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (current != null && Objects.equals(current.getCode(), normalized)) {
            return current;
        }
        return currencyService.requireActive(normalized);
    }

    private static ClientDetails toDetails(ClientRequest request, String email, Currency currency) {
        short terms = request.paymentTermsDays() == null
                ? DEFAULT_PAYMENT_TERMS_DAYS
                : request.paymentTermsDays().shortValue();
        boolean reminders = request.remindersEnabled() == null || request.remindersEnabled();
        return new ClientDetails(
                request.name().trim(),
                email,
                trimToNull(request.billingEmail()),
                trimToNull(request.phone()),
                trimToNull(request.taxId()),
                toAddress(request.address()),
                currency,
                terms,
                reminders);
    }

    /** An address with no values at all is stored as no address. */
    private static Address toAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        Address address = new Address(
                trimToNull(dto.line1()),
                trimToNull(dto.line2()),
                trimToNull(dto.city()),
                trimToNull(dto.state()),
                trimToNull(dto.postalCode()),
                trimToNull(dto.countryCode()));
        boolean empty = address.getLine1() == null && address.getLine2() == null && address.getCity() == null
                && address.getState() == null && address.getPostalCode() == null && address.getCountryCode() == null;
        return empty ? null : address;
    }

    /** Lowercases the text, escapes LIKE wildcards so "50%" matches literally, and wraps it for "contains". */
    static String likePattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ConflictException emailTaken(String email) {
        return new ConflictException("CLIENT_EMAIL_TAKEN", "A client with email " + email + " already exists.");
    }
}
