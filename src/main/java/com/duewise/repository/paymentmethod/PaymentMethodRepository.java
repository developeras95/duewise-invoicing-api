package com.duewise.repository.paymentmethod;

import java.util.List;
import java.util.Optional;

import com.duewise.entity.paymentmethod.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Short> {

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);

    List<PaymentMethod> findAllByOrderBySortOrderAscCodeAsc();

    List<PaymentMethod> findByActiveOrderBySortOrderAscCodeAsc(boolean active);
}
