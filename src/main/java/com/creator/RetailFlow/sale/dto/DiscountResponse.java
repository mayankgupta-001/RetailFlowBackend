package com.creator.RetailFlow.sale.dto;

import com.creator.RetailFlow.sale.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DiscountResponse(
        Long id,
        String name,
        DiscountType type,
        BigDecimal value,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}