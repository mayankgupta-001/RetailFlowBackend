package com.creator.RetailFlow.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StoreSettingsRequest {

    @NotBlank(message = "Shop name is required")
    @Size(max = 255, message = "Shop name must be at most 255 characters")
    private String shopName;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    // Two-digit GST state code, e.g. 19 for West Bengal. Empty is
    // allowed so the admin can clear it from the form.
    @Pattern(
            regexp = "^$|^\\d{2}$",
            message = "State code must be two digits"
    )
    private String stateCode;

    @Size(max = 15, message = "GSTIN must be 15 characters")
    private String gstin;

    public StoreSettingsRequest() {
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }
}
