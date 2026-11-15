package com.duewise.dto.common;

import java.util.List;
import java.util.function.Function;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

/**
 * A stable JSON shape for paginated lists. Returning Spring Data's {@code Page} directly would
 * leak its internal structure into the API and change whenever the library does.
 */
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Zero-based page number", example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(description = "Number of matching items; an estimate when totalExact is false", example = "57")
        long totalElements,
        @Schema(example = "3") int totalPages,
        @Schema(description = "False when totalElements is PostgreSQL's estimate rather than a count: for very "
                + "large unfiltered lists, where counting every row would cost more than the page itself",
                example = "true")
        boolean totalExact) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                true);
    }

    /**
     * A page whose total was estimated, not counted. The estimate is raised if needed so that it is never
     * below what the page itself proves: every row up to this page, plus one more if there is a next page.
     */
    public static <E, T> PageResponse<T> estimated(Slice<E> slice, long estimatedTotal, Function<E, T> mapper) {
        long seen = (long) slice.getNumber() * slice.getSize() + slice.getNumberOfElements() + (slice.hasNext() ? 1 : 0);
        long total = Math.max(estimatedTotal, seen);
        int totalPages = slice.getSize() == 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, (total + slice.getSize() - 1) / slice.getSize());
        return new PageResponse<>(
                slice.getContent().stream().map(mapper).toList(),
                slice.getNumber(),
                slice.getSize(),
                total,
                totalPages,
                false);
    }
}
