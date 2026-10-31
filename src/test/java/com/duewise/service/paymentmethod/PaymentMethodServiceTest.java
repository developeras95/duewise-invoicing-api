package com.duewise.service.paymentmethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.duewise.dto.paymentmethod.CreatePaymentMethodRequest;
import com.duewise.dto.paymentmethod.PaymentMethodResponse;
import com.duewise.dto.paymentmethod.UpdatePaymentMethodRequest;
import com.duewise.entity.paymentmethod.PaymentMethod;
import com.duewise.exception.ConflictException;
import com.duewise.exception.NotFoundException;
import com.duewise.repository.paymentmethod.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    PaymentMethodRepository repository;

    @InjectMocks
    PaymentMethodService service;

    @Test
    void createDefaultsSortOrderToZeroWhenNotSent() {
        when(repository.existsByCode("PAYPAL")).thenReturn(false);
        when(repository.save(any(PaymentMethod.class))).thenAnswer(call -> call.getArgument(0));

        PaymentMethodResponse created =
                service.create(new CreatePaymentMethodRequest("PAYPAL", " PayPal ", true, null));

        assertThat(created).isEqualTo(new PaymentMethodResponse("PAYPAL", "PayPal", true, true, 0));
    }

    @Test
    void createRejectsExistingCode() {
        when(repository.existsByCode("UPI")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreatePaymentMethodRequest("UPI", "UPI", true, 20)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("UPI");
        verify(repository, never()).save(any());
    }

    @Test
    void deactivatingKeepsEverythingElse() {
        PaymentMethod cash = new PaymentMethod("CASH", "Cash", false, (short) 50);
        when(repository.findByCode("CASH")).thenReturn(Optional.of(cash));

        PaymentMethodResponse updated =
                service.update("cash", new UpdatePaymentMethodRequest(null, null, false, null));

        assertThat(updated).isEqualTo(new PaymentMethodResponse("CASH", "Cash", false, false, 50));
    }

    @Test
    void updateAppliesEveryFieldSent() {
        PaymentMethod card = new PaymentMethod("CARD", "Card", true, (short) 30);
        when(repository.findByCode("CARD")).thenReturn(Optional.of(card));

        PaymentMethodResponse updated =
                service.update("CARD", new UpdatePaymentMethodRequest("Credit or debit card", false, true, 5));

        assertThat(updated).isEqualTo(new PaymentMethodResponse("CARD", "Credit or debit card", false, true, 5));
    }

    @Test
    void unknownCodeThrowsNotFound() {
        when(repository.findByCode("BITCOIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("bitcoin"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Payment method BITCOIN was not found.");
    }
}
