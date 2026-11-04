package com.creator.RetailFlow.sale.controller;

import com.creator.RetailFlow.sale.dto.DiscountRequest;
import com.creator.RetailFlow.sale.dto.DiscountResponse;
import com.creator.RetailFlow.sale.dto.DiscountStatusRequest;
import com.creator.RetailFlow.sale.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@PreAuthorize("hasRole('ADMIN')")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(
            DiscountService discountService
    ) {
        this.discountService = discountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(
            @Valid @RequestBody DiscountRequest request
    ) {
        return discountService.create(request);
    }

    @GetMapping
    public List<DiscountResponse> getAll() {
        return discountService.getAll();
    }

    @GetMapping("/{id}")
    public DiscountResponse getById(
            @PathVariable Long id
    ) {
        return discountService.getById(id);
    }

    @PutMapping("/{id}")
    public DiscountResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request
    ) {
        return discountService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public DiscountResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DiscountStatusRequest request
    ) {
        return discountService.updateStatus(
                id,
                request.getActive()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id
    ) {
        discountService.delete(id);
    }
}