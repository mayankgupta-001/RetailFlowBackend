package com.creator.RetailFlow.inventory.dto;

import com.creator.RetailFlow.inventory.entity.InventoryTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StockAdjustmentRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Transaction type is required")
    private InventoryTransactionType type;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    private String reason;

    public StockAdjustmentRequest() {
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

    public String getReason() {
        return reason;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public void setType(InventoryTransactionType type) {
        this.type = type;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}