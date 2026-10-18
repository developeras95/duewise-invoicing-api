package com.duewise.paymentmethod;

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
public class PaymentMethodService {

    static final String CACHE = "paymentMethods";

    private final PaymentMethodRepository repository;

    public PaymentMethodService(PaymentMethodRepository repository) {
        this.repository = repository;
    }

    @Cacheable(cacheNames = CACHE, key = "#active == null ? 'all' : #active.toString()")
    public List<PaymentMethodResponse> list(Boolean active) {
        List<PaymentMethod> methods = active == null
                ? repository.findAllByOrderBySortOrderAscCodeAsc()
                : repository.findByActiveOrderBySortOrderAscCodeAsc(active);
        return methods.stream().map(PaymentMethodResponse::from).toList();
    }

    public PaymentMethodResponse get(String code) {
        return PaymentMethodResponse.from(find(code));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public PaymentMethodResponse create(CreatePaymentMethodRequest request) {
        String code = request.code();
        if (repository.existsByCode(code)) {
            throw new ConflictException("PAYMENT_METHOD_ALREADY_EXISTS",
                    "Payment method " + code + " already exists.");
        }
        short sortOrder = request.sortOrder() == null ? 0 : request.sortOrder().shortValue();
        PaymentMethod method = new PaymentMethod(
                code, request.displayName().trim(), request.requiresReference(), sortOrder);
        return PaymentMethodResponse.from(repository.save(method));
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE, allEntries = true)
    public PaymentMethodResponse update(String code, UpdatePaymentMethodRequest request) {
        PaymentMethod method = find(code);
        if (request.displayName() != null) {
            method.rename(request.displayName().trim());
        }
        if (request.requiresReference() != null) {
            method.setRequiresReference(request.requiresReference());
        }
        if (request.active() != null) {
            method.setActive(request.active());
        }
        if (request.sortOrder() != null) {
            method.setSortOrder(request.sortOrder().shortValue());
        }
        return PaymentMethodResponse.from(method);
    }

    private PaymentMethod find(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return repository.findByCode(normalized)
                .orElseThrow(() -> new NotFoundException(
                        "PAYMENT_METHOD_NOT_FOUND", "Payment method " + normalized + " was not found."));
    }
}
