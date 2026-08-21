package com.creator.RetailFlow.inventory.dto;

import com.creator.RetailFlow.inventory.entity.InventoryTransactionType;

import java.time.LocalDateTime;

public class InventoryLogResponse {

    private final Long id;
    private final Long productId;
    private final InventoryTransactionType type;
    private final Integer quantity;
    private final Integer previousStock;
    private final Integer newStock;
    private final String reason;
    private final LocalDateTime createdAt;

    public InventoryLogResponse(
            Long id,
            Long productId,
            InventoryTransactionType type,
            Integer quantity,
            Integer previousStock,
            Integer newStock,
            String reason,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.previousStock = previousStock;
        this.newStock = newStock;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public InventoryTransactionType getType() {
        return type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getPreviousStock() {
        return previousStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}