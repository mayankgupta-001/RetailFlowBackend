package com.creator.RetailFlow.sale.dto;

import com.creator.RetailFlow.sale.entity.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DiscountRequest {

    @NotBlank(message = "Discount name is required")
    @Size(max = 100, message = "Discount name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Discount type is required")
    private DiscountType type;

    @NotNull(message = "Discount value is required")
    @DecimalMin(
            value = "0.00",
            message = "Discount value cannot be negative"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Discount value must have at most 2 decimal places"
    )
    private BigDecimal value;

    @NotNull(message = "Start date/time is required")
    private LocalDateTime startAt;

    @NotNull(message = "End date/time is required")
    private LocalDateTime endAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DiscountType getType() {
        return type;
    }

    public void setType(DiscountType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }
}