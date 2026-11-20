package com.creator.RetailFlow.settings.dto;

import com.creator.RetailFlow.settings.entity.StoreSettings;

import java.time.LocalDateTime;

/*
 * Shop details plus gstRegistered, so the frontend can decide between
 * "Invoice" and "Tax Invoice" without re-implementing the rule.
 */
public record StoreSettingsResponse(
        Long id,
        String shopName,
        String address,
        String phone,
        String stateCode,
        String gstin,
        boolean gstRegistered,
        LocalDateTime updatedAt
) {

    public static StoreSettingsResponse from(StoreSettings settings) {

        boolean registered = settings.getGstin() != null
                && !settings.getGstin().isBlank();

        return new StoreSettingsResponse(
                settings.getId(),
                settings.getShopName(),
                settings.getAddress(),
                settings.getPhone(),
                settings.getStateCode(),
                settings.getGstin(),
                registered,
                settings.getUpdatedAt()
        );
    }
}
