package com.creator.RetailFlow.sale.dto;

import com.creator.RetailFlow.sale.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CheckoutItemRequest> items;

    @NotNull(message = "Discount is required")
    @DecimalMin(value = "0.00", message = "Discount cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Discount must have at most 2 decimal places")
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    public List<CheckoutItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CheckoutItemRequest> items) {
        this.items = items;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}