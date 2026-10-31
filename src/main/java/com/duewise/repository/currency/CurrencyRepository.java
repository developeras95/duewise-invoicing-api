package com.duewise.repository.currency;

import java.util.List;

import com.duewise.entity.currency.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findAllByOrderByCodeAsc();

    List<Currency> findByActiveOrderByCodeAsc(boolean active);
}
