package com.creator.RetailFlow.discount.dto;

import jakarta.validation.constraints.NotNull;

public class DiscountStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
