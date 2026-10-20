package com.duewise.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.BusinessRuleException;
import com.duewise.common.error.ConflictException;
import com.duewise.common.error.NotFoundException;
import com.duewise.currency.Currency;
import com.duewise.currency.CurrencyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    ClientRepository repository;

    @Mock
    CurrencyService currencyService;

    @InjectMocks
    ClientService service;

    @Test
    void createAppliesDefaultsAndTrimsText() {
        when(repository.existsByEmailIgnoreCase("asha@acme.in")).thenReturn(false);
        when(repository.save(any(Client.class))).thenAnswer(call -> call.getArgument(0));

        ClientResponse created = service.create(request("  Acme  ", " asha@acme.in ", null, null));

        assertThat(created.name()).isEqualTo("Acme");
        assertThat(created.email()).isEqualTo("asha@acme.in");
        assertThat(created.paymentTermsDays()).isEqualTo(30);
        assertThat(created.remindersEnabled()).isTrue();
        assertThat(created.address()).isNull();
        verifyNoInteractions(currencyService);
    }

    @Test
    void createRejectsEmailAlreadyUsedByAnotherClient() {
        when(repository.existsByEmailIgnoreCase("asha@acme.in")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("Acme", "asha@acme.in", null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("asha@acme.in");
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsInactiveCurrency() {
        when(repository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(currencyService.requireActive("GBP"))
                .thenThrow(new BusinessRuleException("CURRENCY_INACTIVE", "Currency GBP is inactive and cannot be used."));

        assertThatThrownBy(() -> service.create(request("Acme", "asha@acme.in", "gbp", null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateWithoutVersionIsRejected() {
        assertThatThrownBy(() -> service.update(1L, request("Acme", "asha@acme.in", null, null)))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void updateWithStaleVersionIsRejected() {
        Client stored = new Client(details("Acme", "asha@acme.in", null));   // version 0
        when(repository.findById(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.update(1L, request("Acme Ltd", "asha@acme.in", null, 3L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("changed by someone else");
    }

    @Test
    void updateRejectsEmailOfAnotherClient() {
        Client stored = new Client(details("Acme", "asha@acme.in", null));
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.existsByEmailIgnoreCaseAndIdNot("ravi@other.in", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, request("Acme", "ravi@other.in", null, 0L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateKeepsCurrentCurrencyEvenIfItWasDeactivated() {
        Currency inactiveGbp = new Currency("GBP", "Pound Sterling", "£", (short) 2);
        inactiveGbp.setActive(false);
        Client stored = new Client(details("Acme", "asha@acme.in", inactiveGbp));
        when(repository.findById(1L)).thenReturn(Optional.of(stored));
        when(repository.existsByEmailIgnoreCaseAndIdNot("asha@acme.in", 1L)).thenReturn(false);
        when(repository.saveAndFlush(stored)).thenReturn(stored);

        ClientResponse updated = service.update(1L, request("Acme Ltd", "asha@acme.in", "GBP", 0L));

        assertThat(updated.name()).isEqualTo("Acme Ltd");
        assertThat(updated.defaultCurrencyCode()).isEqualTo("GBP");
        verifyNoInteractions(currencyService);
    }

    @Test
    void getUnknownClientThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Client 99 was not found.");
    }

    @Test
    void searchEscapesWildcardsTypedByTheUser() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.search(eq("%50\\%\\_off%"), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.search(" 50%_OFF ", pageable);

        verify(repository).search("%50\\%\\_off%", pageable);
    }

    @Test
    void blankSearchListsEveryone() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.search("   ", pageable).totalElements()).isZero();
        verify(repository, never()).search(any(), any());
    }

    private static ClientRequest request(String name, String email, String currency, Long version) {
        return new ClientRequest(name, email, null, null, null, null, currency, null, null, version);
    }

    private static ClientDetails details(String name, String email, Currency currency) {
        return new ClientDetails(name, email, null, null, null, null, currency, (short) 30, true);
    }
}
