package com.creator.RetailFlow.sale.controller;

import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.dto.SaleItemResponse;
import com.creator.RetailFlow.sale.dto.SaleResponse;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales" )
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<SaleResponse> checkout(
            @Valid @RequestBody CheckoutRequest request) {

        Sale sale = saleService.checkout(request);

        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setInvoiceNumber(sale.getInvoiceNumber());
        response.setSubtotal(sale.getSubtotal());
        response.setDiscount(sale.getDiscount());
        response.setTotal(sale.getTotal());
        response.setPaymentMethod(sale.getPaymentMethod());
        response.setCreatedAt(sale.getCreatedAt());

        List<SaleItemResponse> itemResponses = sale.getItems()
                .stream()
                .map(item -> {
                    SaleItemResponse itemResponse = new SaleItemResponse();
                    itemResponse.setProductName(item.getProductName());
                    itemResponse.setBarcode(item.getBarcode());
                    itemResponse.setPrice(item.getPrice());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setSubtotal(item.getSubtotal());
                    return itemResponse;
                })
                .toList();

        response.setItems(itemResponses);

        return ResponseEntity.ok(response);
    }
}
