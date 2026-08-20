package com.creator.RetailFlow.product.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private String barcode;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice;
    private Integer stock;
    private Integer minStock;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductResponse(
            Long id,
            String name,
            String barcode,
            BigDecimal sellingPrice,
            BigDecimal costPrice,
            Integer stock,
            Integer minStock,
            String category,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.barcode = barcode;
        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;
        this.stock = stock;
        this.minStock = minStock;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
