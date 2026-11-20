package com.creator.RetailFlow.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal costPrice;

    @NotNull(message = "Minimum stock is required")
    @PositiveOrZero
    private Integer minStock;

    private String category;

    // Optional: a null taxRate keeps the stored value, so older clients
    // that do not send the field cannot silently zero a product's GST.
    @DecimalMin(value = "0.0", message = "Tax rate cannot be negative")
    @DecimalMax(value = "100.0", message = "Tax rate cannot exceed 100")
    @Digits(
            integer = 3,
            fraction = 2,
            message = "Tax rate must have at most 2 decimal places"
    )
    private BigDecimal taxRate;

    // Blank clears the stored HSN code.
    @Size(max = 8, message = "HSN code must be at most 8 characters")
    private String hsnCode;

    public UpdateProductRequest() {
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public String getHsnCode() {
        return hsnCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setHsnCode(String hsnCode) {
        this.hsnCode = hsnCode;
    }
}