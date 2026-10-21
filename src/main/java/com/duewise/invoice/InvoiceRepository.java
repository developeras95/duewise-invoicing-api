package com.duewise.invoice;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Loads client and currency in the same query, so a page of 20 invoices is 1 query, not 41. */
    @Override
    @EntityGraph(attributePaths = {"client", "currency"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    /** Single invoice with its client, currency and lines, for the detail view. */
    @EntityGraph(attributePaths = {"client", "currency", "lines"})
    Optional<Invoice> findWithDetailsById(Long id);
}
