package com.duewise.common.web;

import java.util.List;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * A stable JSON shape for paginated lists. Returning Spring Data's {@code Page} directly would
 * leak its internal structure into the API and change whenever the library does.
 */
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Zero-based page number", example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "57") long totalElements,
        @Schema(example = "3") int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
