package com.creator.RetailFlow.sale.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/*
 * Stable JSON shape for paginated API responses, so the frontend never
 * depends on Spring's internal PageImpl serialization format.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
