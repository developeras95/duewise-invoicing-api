package com.duewise.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Case-insensitive "contains" search on name or email. The pattern arrives already lowercased,
     * wrapped in % and with any %, _ or \ typed by the user escaped, so they match literally.
     */
    @Query("""
            select c from Client c
            where lower(c.name) like :pattern escape '\\'
               or lower(c.email) like :pattern escape '\\'
            """)
    Page<Client> search(@Param("pattern") String pattern, Pageable pageable);
}
