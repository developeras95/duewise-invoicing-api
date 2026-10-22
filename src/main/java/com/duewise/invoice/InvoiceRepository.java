package com.duewise.invoice;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    /** Loads client and currency in the same query, so a page of 20 invoices is 1 query, not 41. */
    @Override
    @EntityGraph(attributePaths = {"client", "currency"})
    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    /** Single invoice with its client, currency and lines, for the detail view. */
    @EntityGraph(attributePaths = {"client", "currency", "lines"})
    Optional<Invoice> findWithDetailsById(Long id);

    /** Every invoice with money owed past its due date, fully loaded for emails and PDFs. */
    @Query("""
            select distinct i from Invoice i
            join fetch i.client
            join fetch i.currency
            left join fetch i.lines
            where i.status in :statuses and i.dueDate < :today
            order by i.dueDate, i.id
            """)
    List<Invoice> findOverdueWithDetails(@Param("statuses") Collection<InvoiceStatus> statuses,
                                         @Param("today") LocalDate today);
}
