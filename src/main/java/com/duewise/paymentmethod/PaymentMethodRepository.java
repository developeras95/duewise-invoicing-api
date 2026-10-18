package com.duewise.paymentmethod;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Short> {

    Optional<PaymentMethod> findByCode(String code);

    boolean existsByCode(String code);

    List<PaymentMethod> findAllByOrderBySortOrderAscCodeAsc();

    List<PaymentMethod> findByActiveOrderBySortOrderAscCodeAsc(boolean active);
}
