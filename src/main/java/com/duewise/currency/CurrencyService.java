package com.duewise.currency;

import java.util.List;
import java.util.Locale;

import com.duewise.common.error.ConflictException;
import com.duewise.common.error.NotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurrencyService {

    static final String CACHE = "currencies";

    private final CurrencyRepository repository;

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists currencies, optionally only active or only inactive ones. Cached because the list is
     * small, rarely changes and is read on every invoice form.
     */
    @Cacheable(cacheNames = CACHE, key = "#active == null ? 'all' : #active.toString()")
    public List<CurrencyResponse> list(Boolean active) {
        List<Currency> currencies = active == null
                ? repository.findAllByOrderByCodeAsc()
                : repository.findByActiveOrderByCodeAsc(active);
        return currencies.stream().map(CurrencyResponse::from).toList();
    }

    public CurrencyResponse get(String code) {
        return CurrencyResponse.from(find(code));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public CurrencyResponse create(CreateCurrencyRequest request) {
        String code = request.code();
        if (repository.existsById(code)) {
            throw new ConflictException("CURRENCY_ALREADY_EXISTS", "Currency " + code + " already exists.");
        }
        Currency currency = new Currency(
                code,
                request.name().trim(),
                request.symbol().trim(),
                request.minorUnits().shortValue());
        return CurrencyResponse.from(repository.save(currency));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public CurrencyResponse update(String code, UpdateCurrencyRequest request) {
        Currency currency = find(code);
        if (request.name() != null) {
            currency.rename(request.name().trim());
        }
        if (request.symbol() != null) {
            currency.changeSymbol(request.symbol().trim());
        }
        if (request.active() != null) {
            currency.setActive(request.active());
        }
        return CurrencyResponse.from(currency);
    }

    private Currency find(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.findById(normalized)
                .orElseThrow(() -> new NotFoundException(
                        "CURRENCY_NOT_FOUND", "Currency " + normalized + " was not found."));
    }
}
