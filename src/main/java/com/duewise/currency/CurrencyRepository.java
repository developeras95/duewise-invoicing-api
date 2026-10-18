package com.duewise.currency;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findAllByOrderByCodeAsc();

    List<Currency> findByActiveOrderByCodeAsc(boolean active);
}
