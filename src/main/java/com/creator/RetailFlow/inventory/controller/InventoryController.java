package com.creator.RetailFlow.inventory.controller;

import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.creator.RetailFlow.inventory.dto.InventoryLogResponse;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.OK)
    public void adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        inventoryService.adjustStock(request);
    }
    @GetMapping("/logs/product/{productId}")
    public List<InventoryLogResponse> getProductInventoryHistory(
            @PathVariable Long productId
    ) {
        return inventoryService.getProductInventoryHistory(productId);
    }
}