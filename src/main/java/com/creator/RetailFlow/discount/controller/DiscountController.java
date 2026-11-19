package com.creator.RetailFlow.discount.controller;

import com.creator.RetailFlow.discount.dto.DiscountRequest;
import com.creator.RetailFlow.discount.dto.DiscountResponse;
import com.creator.RetailFlow.discount.dto.DiscountStatusRequest;
import com.creator.RetailFlow.discount.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * Reading discounts is open to any logged-in user (ADMIN or CASHIER)
 * so the checkout screen can list applicable discounts. Managing them
 * (create/update/status/delete) is ADMIN-only, per issue #15.
 */
@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<DiscountResponse> create(
            @Valid @RequestBody DiscountRequest request
    ) {
        DiscountResponse response = DiscountResponse.from(
                discountService.create(request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<DiscountResponse> list() {
        return discountService.list()
                .stream()
                .map(DiscountResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DiscountResponse get(@PathVariable Long id) {
        return DiscountResponse.from(discountService.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public DiscountResponse update(
            @PathVariable Long id,
            @Valid @RequestBody DiscountRequest request
    ) {
        return DiscountResponse.from(
                discountService.update(id, request)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public DiscountResponse patchStatus(
            @PathVariable Long id,
            @Valid @RequestBody DiscountStatusRequest request
    ) {
        return DiscountResponse.from(
                discountService.patchStatus(id, request.getActive())
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
