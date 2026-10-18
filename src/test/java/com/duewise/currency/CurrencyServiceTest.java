package com.duewise.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.duewise.common.error.ConflictException;
import com.duewise.common.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    CurrencyRepository repository;

    @InjectMocks
    CurrencyService service;

    @Test
    void listWithoutFilterReturnsAllCurrencies() {
        when(repository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                new Currency("INR", "Indian Rupee", "₹", (short) 2),
                new Currency("JPY", "Japanese Yen", "¥", (short) 0)));

        List<CurrencyResponse> result = service.list(null);

        assertThat(result).extracting(CurrencyResponse::code).containsExactly("INR", "JPY");
        assertThat(result.get(1).minorUnits()).isZero();
    }

    @Test
    void listWithFilterAsksRepositoryForThatStatus() {
        when(repository.findByActiveOrderByCodeAsc(false)).thenReturn(List.of());

        assertThat(service.list(false)).isEmpty();
        verify(repository).findByActiveOrderByCodeAsc(false);
    }

    @Test
    void createSavesNewCurrencyAsActiveWithTrimmedText() {
        when(repository.existsById("CHF")).thenReturn(false);
        when(repository.save(any(Currency.class))).thenAnswer(call -> call.getArgument(0));

        CurrencyResponse created = service.create(new CreateCurrencyRequest("CHF", "  Swiss Franc ", " CHF", 2));

        assertThat(created).isEqualTo(new CurrencyResponse("CHF", "Swiss Franc", "CHF", 2, true));
    }

    @Test
    void createRejectsExistingCode() {
        when(repository.existsById("INR")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCurrencyRequest("INR", "Indian Rupee", "₹", 2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("INR");
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesOnlyTheFieldsSent() {
        Currency inr = new Currency("INR", "Indian Rupee", "₹", (short) 2);
        when(repository.findById("INR")).thenReturn(Optional.of(inr));

        CurrencyResponse updated = service.update("INR", new UpdateCurrencyRequest(null, null, false));

        assertThat(updated.active()).isFalse();
        assertThat(updated.name()).isEqualTo("Indian Rupee");
        assertThat(updated.symbol()).isEqualTo("₹");
    }

    @Test
    void lookupIgnoresCaseOfCodeInPath() {
        when(repository.findById("USD")).thenReturn(Optional.of(new Currency("USD", "US Dollar", "$", (short) 2)));

        assertThat(service.get("usd").code()).isEqualTo("USD");
    }

    @Test
    void unknownCodeThrowsNotFound() {
        when(repository.findById("XYZ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("XYZ"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Currency XYZ was not found.");
    }
}
