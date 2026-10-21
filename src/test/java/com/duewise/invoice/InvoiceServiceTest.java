package com.duewise.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.duewise.client.Client;
import com.duewise.client.ClientService;
import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.ConflictException;
import com.duewise.currency.Currency;
import com.duewise.currency.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");
    /** 20 October 2026, 10:30 in India. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T05:00:00Z"), KOLKATA);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static final Currency INR = new Currency("INR", "Indian Rupee", "₹", (short) 2);
    private static final Currency JPY = new Currency("JPY", "Japanese Yen", "¥", (short) 0);

    @Mock InvoiceRepository repository;
    @Mock ClientService clientService;
    @Mock CurrencyService currencyService;
    @Mock InvoiceNumberGenerator numberGenerator;
    @Mock Client client;

    InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(repository, clientService, currencyService, numberGenerator, CLOCK);
    }

    @Test
    void createTakesCurrencyAndDatesFromTheClient() {
        when(clientService.require(1L)).thenReturn(client);
        when(client.getDefaultCurrency()).thenReturn(INR);
        when(client.getPaymentTermsDays()).thenReturn((short) 15);
        when(currencyService.requireActive("INR")).thenReturn(INR);
        when(repository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        InvoiceResponse created = service.create(request(null, null, null,
                line("2", "12500.50", "18"), line("1.5", "999.99", null)));

        assertThat(created.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(created.currencyCode()).isEqualTo("INR");
        assertThat(created.issueDate()).isEqualTo(TODAY);
        assertThat(created.dueDate()).isEqualTo(TODAY.plusDays(15));
        assertThat(created.total()).isEqualByComparingTo("31001.17");
        assertThat(created.total().scale()).as("shown with INR's 2 decimal places").isEqualTo(2);
        assertThat(created.lines().get(1).taxRate()).isEqualByComparingTo("0");
    }

    @Test
    void createRejectsPriceWithMoreDecimalsThanTheCurrencyAllows() {
        when(clientService.require(1L)).thenReturn(client);
        when(currencyService.requireActive("JPY")).thenReturn(JPY);

        assertThatThrownBy(() -> service.create(request("JPY", null, null, line("1", "1500.50", "10"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lines[0].unitPrice")
                .hasMessageContaining("JPY");
        verify(repository, never()).save(any());
    }

    @Test
    void createWithoutAnyCurrencyIsRejected() {
        when(clientService.require(1L)).thenReturn(client);
        when(client.getDefaultCurrency()).thenReturn(null);

        assertThatThrownBy(() -> service.create(request(null, null, null, line("1", "100", "0"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("currencyCode");
    }

    @Test
    void dueDateBeforeIssueDateIsRejected() {
        when(clientService.require(1L)).thenReturn(client);
        when(currencyService.requireActive("INR")).thenReturn(INR);

        assertThatThrownBy(() -> service.create(request("INR", TODAY, TODAY.minusDays(1), line("1", "100", "0"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dueDate");
    }

    @Test
    void sentInvoiceCannotBeUpdated() {
        Invoice sent = new Invoice(client, INR, TODAY, TODAY.plusDays(30), null);
        sent.replaceLines(List.of(new LineDraft("Work", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO)), 2);
        sent.send(() -> "INV-2026-00001", Instant.now(CLOCK));
        when(repository.findWithDetailsById(5L)).thenReturn(Optional.of(sent));

        InvoiceRequest update = new InvoiceRequest(1L, "INR", null, null, null, List.of(line("1", "200", "0")), 0L);

        assertThatThrownBy(() -> service.update(5L, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only drafts can be changed");
    }

    @Test
    void sendUsesTheIssueYearForTheNumber() {
        Invoice draft = new Invoice(client, INR, LocalDate.of(2025, 12, 31), LocalDate.of(2026, 1, 30), null);
        draft.replaceLines(List.of(new LineDraft("Work", BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO)), 2);
        when(repository.findWithDetailsById(7L)).thenReturn(Optional.of(draft));
        when(numberGenerator.next(2025)).thenReturn("INV-2025-00042");
        when(repository.saveAndFlush(draft)).thenReturn(draft);

        InvoiceResponse sent = service.send(7L);

        assertThat(sent.invoiceNumber()).isEqualTo("INV-2025-00042");
        assertThat(sent.status()).isEqualTo(InvoiceStatus.SENT);
        assertThat(sent.sentAt()).isEqualTo(Instant.now(CLOCK));
    }

    @Test
    void sendingAnEmptyDraftNeverTouchesTheCounter() {
        Invoice empty = new Invoice(client, INR, TODAY, TODAY.plusDays(30), null);
        when(repository.findWithDetailsById(8L)).thenReturn(Optional.of(empty));

        assertThatThrownBy(() -> service.send(8L)).hasMessageContaining("at least one line");
        verify(numberGenerator, never()).next(anyInt());
    }

    @Test
    void invertedDateRangeInListIsRejected() {
        InvoiceFilter filter = new InvoiceFilter(null, null, false, TODAY, TODAY.minusDays(1));

        assertThatThrownBy(() -> service.list(filter, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(BadRequestException.class);
    }

    private static InvoiceRequest request(String currency, LocalDate issue, LocalDate due, LineItemRequest... lines) {
        return new InvoiceRequest(1L, currency, issue, due, null, List.of(lines), null);
    }

    private static LineItemRequest line(String quantity, String unitPrice, String taxRate) {
        return new LineItemRequest("Work", new BigDecimal(quantity), new BigDecimal(unitPrice),
                taxRate == null ? null : new BigDecimal(taxRate));
    }
}
