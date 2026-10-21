package com.duewise.invoice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

/** Builds the WHERE clause for the invoice list from whichever filters were sent. */
final class InvoiceSpecifications {

    private InvoiceSpecifications() {
    }

    static Specification<Invoice> matching(InvoiceFilter filter, LocalDate today) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.clientId() != null) {
                predicates.add(cb.equal(root.get("client").get("id"), filter.clientId()));
            }
            if (filter.overdueOnly()) {
                // Same rule as Invoice.isOverdue: money still owed and the due date has passed.
                predicates.add(root.get("status").in(InvoiceStatus.SENT, InvoiceStatus.PARTIALLY_PAID));
                predicates.add(cb.lessThan(root.<LocalDate>get("dueDate"), today));
            }
            if (filter.issuedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("issueDate"), filter.issuedFrom()));
            }
            if (filter.issuedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("issueDate"), filter.issuedTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
