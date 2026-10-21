package com.duewise.payment;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Payments on one invoice, oldest first, with their method loaded in the same query. */
    @EntityGraph(attributePaths = "paymentMethod")
    List<Payment> findByInvoiceIdOrderByPaidOnAscIdAsc(Long invoiceId);
}
