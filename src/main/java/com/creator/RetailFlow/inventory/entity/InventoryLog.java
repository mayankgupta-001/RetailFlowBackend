package com.creator.RetailFlow.inventory.entity;

import com.creator.RetailFlow.product.entity.Product;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryTransactionType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    private String reason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public InventoryLog() {
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
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

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setType(InventoryTransactionType type) {
        this.type = type;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setPreviousStock(Integer previousStock) {
        this.previousStock = previousStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}