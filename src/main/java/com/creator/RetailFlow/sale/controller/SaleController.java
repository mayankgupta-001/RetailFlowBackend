package com.creator.RetailFlow.sale.controller;

import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.dto.SaleItemResponse;
import com.creator.RetailFlow.sale.dto.SaleResponse;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

    private SaleResponse toResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setInvoiceNumber(sale.getInvoiceNumber());
        response.setSubtotal(sale.getSubtotal());
        response.setDiscount(sale.getDiscount());
        response.setTotal(sale.getTotal());
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
                    return itemResponse;
                })
                .toList();

        response.setItems(itemResponses);
        return response;
    }
    @GetMapping
    public List<SaleResponse> getSalesBetween(
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        LocalDateTime start = LocalDateTime.parse(from);
        LocalDateTime end = LocalDateTime.parse(to);
        return saleService.getSalesBetween(start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    @PostMapping("/{id}/void")
    public ResponseEntity<SaleResponse> voidSale(@PathVariable Long id) {
        Sale sale = saleService.voidSale(id);
        return ResponseEntity.ok(toResponse(sale));
    }
}