package com.duewise.invoice;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.duewise.client.Client;
import com.duewise.client.ClientService;
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
public class InvoiceService {

    private final InvoiceRepository repository;
    private final ClientService clientService;
    private final CurrencyService currencyService;
    private final InvoiceNumberGenerator numberGenerator;
    private final Clock clock;

    public InvoiceService(InvoiceRepository repository, ClientService clientService, CurrencyService currencyService,
                          InvoiceNumberGenerator numberGenerator, Clock clock) {
        this.repository = repository;
        this.clientService = clientService;
        this.currencyService = currencyService;
        this.numberGenerator = numberGenerator;
        this.clock = clock;
    }

    // ----- Queries --------------------------------------------------------------------------

    public PageResponse<InvoiceSummaryResponse> list(InvoiceFilter filter, Pageable pageable) {
        if (filter.issuedFrom() != null && filter.issuedTo() != null && filter.issuedFrom().isAfter(filter.issuedTo())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "issuedFrom must be on or before issuedTo.");
        }
        LocalDate today = today();
        Page<Invoice> page = repository.findAll(InvoiceSpecifications.matching(filter, today), pageable);
        return PageResponse.from(page, invoice -> InvoiceSummaryResponse.from(invoice, today));
    }

    public InvoiceResponse get(Long id) {
        return InvoiceResponse.from(find(id), today());
    }

    // ----- Drafts ---------------------------------------------------------------------------

    @Transactional
    public InvoiceResponse create(InvoiceRequest request) {
        Client client = clientService.require(request.clientId());
        Currency currency = resolveCurrency(request.currencyCode(), client, null);
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : today();
        LocalDate dueDate = resolveDueDate(request.dueDate(), issueDate, client);
        List<LineDraft> lines = toLineDrafts(request.lines(), currency);

        Invoice invoice = new Invoice(client, currency, issueDate, dueDate, trimToNull(request.notes()));
        invoice.replaceLines(lines, currency.getMinorUnits());
        checkTotalFits(invoice);
        return InvoiceResponse.from(repository.save(invoice), today());
    }

    /** Replaces a draft completely: header fields and all lines. */
    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        if (request.version() == null) {
            throw new BadRequestException("VERSION_REQUIRED",
                    "Send the invoice's current 'version' when updating, as returned by GET.");
        }
        Invoice invoice = find(id);
        if (invoice.getVersion() != request.version()) {
            throw new ConflictException("CONCURRENT_MODIFICATION",
                    "Invoice " + id + " was changed by someone else (now at version " + invoice.getVersion()
                            + "). Reload it and try again.");
        }
        invoice.ensureEditable();

        Client client = Objects.equals(invoice.getClient().getId(), request.clientId())
                ? invoice.getClient()
                : clientService.require(request.clientId());
        Currency currency = resolveCurrency(request.currencyCode(), client, invoice.getCurrency());
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : invoice.getIssueDate();
        LocalDate dueDate = resolveDueDate(request.dueDate(), issueDate, client);
        List<LineDraft> lines = toLineDrafts(request.lines(), currency);

        invoice.updateDraft(client, currency, issueDate, dueDate, trimToNull(request.notes()));
        invoice.replaceLines(lines, currency.getMinorUnits());
        checkTotalFits(invoice);
        return InvoiceResponse.from(repository.saveAndFlush(invoice), today());
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = find(id);
        invoice.ensureDeletable();
        repository.delete(invoice);
    }

    // ----- Status changes -------------------------------------------------------------------

    /** Issues the invoice: assigns the next number for its issue year and locks it against edits. */
    @Transactional
    public InvoiceResponse send(Long id) {
        Invoice invoice = find(id);
        int year = invoice.getIssueDate().getYear();
        invoice.send(() -> numberGenerator.next(year), Instant.now(clock));
        return InvoiceResponse.from(repository.saveAndFlush(invoice), today());
    }

    @Transactional
    public InvoiceResponse voidInvoice(Long id, VoidInvoiceRequest request) {
        Invoice invoice = find(id);
        invoice.voidInvoice(request.reason().trim(), Instant.now(clock));
        return InvoiceResponse.from(repository.saveAndFlush(invoice), today());
    }

    // ----- Helpers --------------------------------------------------------------------------

    private Invoice find(Long id) {
        return repository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("INVOICE_NOT_FOUND", "Invoice " + id + " was not found."));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * Uses the currency sent, otherwise the client's default. A new choice must be active; a draft
     * that already uses a since-deactivated currency may keep it.
     */
    private Currency resolveCurrency(String requestedCode, Client client, Currency current) {
        String code = trimToNull(requestedCode);
        if (code == null && client.getDefaultCurrency() != null) {
            code = client.getDefaultCurrency().getCode();
        }
        if (code == null) {
            throw new BadRequestException("CURRENCY_REQUIRED",
                    "Send a currencyCode, or set a default currency on the client.");
        }
        code = code.toUpperCase(Locale.ROOT);
        if (current != null && current.getCode().equals(code)) {
            return current;
        }
        return currencyService.requireActive(code);
    }

    private static LocalDate resolveDueDate(LocalDate requested, LocalDate issueDate, Client client) {
        LocalDate dueDate = requested != null ? requested : issueDate.plusDays(client.getPaymentTermsDays());
        if (dueDate.isBefore(issueDate)) {
            throw new BadRequestException("INVALID_DUE_DATE", "dueDate cannot be before issueDate.");
        }
        return dueDate;
    }

    private static List<LineDraft> toLineDrafts(List<LineItemRequest> lines, Currency currency) {
        if (lines == null) {
            return List.of();
        }
        List<LineDraft> drafts = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            LineItemRequest line = lines.get(i);
            if (InvoiceMath.exceedsPrecision(line.unitPrice(), currency.getMinorUnits())) {
                throw new BadRequestException("INVALID_AMOUNT_PRECISION",
                        "lines[%d].unitPrice has more decimal places than %s allows (%d)."
                                .formatted(i, currency.getCode(), currency.getMinorUnits()));
            }
            BigDecimal taxRate = line.taxRate() != null ? line.taxRate() : BigDecimal.ZERO;
            drafts.add(new LineDraft(line.description().trim(), line.quantity(), line.unitPrice(), taxRate));
        }
        return drafts;
    }

    private static void checkTotalFits(Invoice invoice) {
        if (invoice.getTotal().compareTo(InvoiceMath.MAX_AMOUNT) > 0) {
            throw new BusinessRuleException("AMOUNT_TOO_LARGE",
                    "The invoice total is larger than the maximum supported amount.");
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
