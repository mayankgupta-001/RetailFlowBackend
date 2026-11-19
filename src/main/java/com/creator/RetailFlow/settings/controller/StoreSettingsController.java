package com.creator.RetailFlow.settings.controller;

import com.creator.RetailFlow.settings.dto.StoreSettingsRequest;
import com.creator.RetailFlow.settings.dto.StoreSettingsResponse;
import com.creator.RetailFlow.settings.service.StoreSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Reading store details is open to any logged-in user because the
 * cashier's receipt needs the shop name, address and GSTIN. Editing
 * them is ADMIN-only, matching the discounts pattern.
 */
@RestController
@RequestMapping("/api/settings")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(
            StoreSettingsService storeSettingsService
    ) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping("/store")
    public StoreSettingsResponse getStore() {
        return StoreSettingsResponse.from(storeSettingsService.get());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/store")
    public StoreSettingsResponse updateStore(
            @Valid @RequestBody StoreSettingsRequest request
    ) {
        return StoreSettingsResponse.from(
                storeSettingsService.update(request)
        );
    }
}
