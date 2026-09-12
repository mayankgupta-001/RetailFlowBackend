package com.creator.RetailFlow.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class CreateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    // No longer required — if omitted, the backend auto-generates
    // an EAN-13 barcode.
    private String barcode;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal costPrice;

    @NotNull(message = "Stock is required")
    @PositiveOrZero
    private Integer stock;

    @NotNull(message = "Minimum stock is required")
    @PositiveOrZero
    private Integer minStock;

    private String category;

    public CreateProductRequest() {
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

    public void setName(String name) {
        this.name = name;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}