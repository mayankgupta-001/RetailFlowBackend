package com.creator.RetailFlow.sale.controller;

import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.dto.PageResponse;
import com.creator.RetailFlow.sale.dto.SaleItemResponse;
import com.creator.RetailFlow.sale.dto.SaleResponse;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody CheckoutRequest request) {
        Sale sale = saleService.checkout(request);
        return ResponseEntity.ok(toResponse(sale));
    }

    // Open to any logged-in user (ADMIN or CASHIER): finds past invoices by
    // partial invoice number so they can be reprinted. Returns at most 20.
    @GetMapping("/lookup")
    public List<SaleResponse> lookup(
            @RequestParam(value = "query", defaultValue = "") String query) {
        return saleService.lookupSales(query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getById(@PathVariable Long id) {
        Sale sale = saleService.getById(id);
        return ResponseEntity.ok(toResponse(sale));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<SaleResponse> getByInvoiceNumber(
            @PathVariable String invoiceNumber) {
        Sale sale = saleService.getByInvoiceNumber(invoiceNumber);
        return ResponseEntity.ok(toResponse(sale));
    }

    // Revenue reports stay ADMIN-only.
    //
    // Paginated so the payload stays bounded no matter how large the
    // date range is. Response shape: PageResponse
    // (content, page, size, totalElements, totalPages).
    //
    // Dates are ISO-8601, e.g. from=2026-01-01T00:00:00
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<SaleResponse> getSalesBetween(
            @RequestParam("from")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam("to")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "'page' must not be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "'size' must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<SaleResponse> responsePage = saleService
                .getSalesBetween(from, to, pageable)
                .map(this::toResponse);

        return PageResponse.from(responsePage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/void")
    public ResponseEntity<SaleResponse> voidSale(@PathVariable Long id) {
        Sale sale = saleService.voidSale(id);
        return ResponseEntity.ok(toResponse(sale));
    }

    private SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setInvoiceNumber(sale.getInvoiceNumber());
        response.setSubtotal(sale.getSubtotal());
        response.setDiscount(sale.getDiscount());
        response.setTotal(sale.getTotal());
        response.setTaxableAmount(sale.getTaxableAmount());
        response.setTaxAmount(sale.getTaxAmount());
        response.setCgstAmount(sale.getCgstAmount());
        response.setSgstAmount(sale.getSgstAmount());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setCreatedAt(sale.getCreatedAt());
        response.setVoided(sale.isVoided());

        List<SaleItemResponse> itemResponses = sale.getItems()
                .stream()
                .map(item -> {
                    SaleItemResponse itemResponse = new SaleItemResponse();
                    itemResponse.setId(item.getId());
                    itemResponse.setProductId(
                            item.getProduct() != null
                                    ? item.getProduct().getId()
                                    : null
                    );
                    itemResponse.setProductName(item.getProductName());
                    itemResponse.setBarcode(item.getBarcode());
                    itemResponse.setPrice(item.getPrice());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setSubtotal(item.getSubtotal());
                    itemResponse.setTaxRate(item.getTaxRate());
                    itemResponse.setHsnCode(item.getHsnCode());
                    itemResponse.setTaxableAmount(item.getTaxableAmount());
                    itemResponse.setTaxAmount(item.getTaxAmount());
                    itemResponse.setCgstAmount(item.getCgstAmount());
                    itemResponse.setSgstAmount(item.getSgstAmount());
                    return itemResponse;
                })
                .toList();

        response.setItems(itemResponses);
        return response;
    }
}