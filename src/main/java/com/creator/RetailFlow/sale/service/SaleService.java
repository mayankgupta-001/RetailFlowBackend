package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.entity.InventoryTransactionType;
import com.creator.RetailFlow.inventory.service.InventoryService;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.sale.dto.CheckoutItemRequest;
import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.entity.SaleItem;
import com.creator.RetailFlow.sale.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public SaleService(
            SaleRepository saleRepository,
            ProductRepository productRepository,
            InventoryService inventoryService
    ) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Sale checkout(CheckoutRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "A sale must contain at least one item"
            );
        }

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutItemRequest itemRequest : request.getItems()) {

            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found with id: "
                                            + itemRequest.getProductId()
                            )
                    );

            int quantity = itemRequest.getQuantity();

            if (product.getStock() < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            // Calculate item subtotal (using price BEFORE stock is adjusted)
            BigDecimal itemSubtotal =
                    product.getSellingPrice()
                            .multiply(BigDecimal.valueOf(quantity));

            SaleItem saleItem = new SaleItem();

            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setProductName(product.getName());
            saleItem.setBarcode(product.getBarcode());
            saleItem.setPrice(product.getSellingPrice());
            saleItem.setQuantity(quantity);
            saleItem.setSubtotal(itemSubtotal);

            sale.getItems().add(saleItem);

            subtotal = subtotal.add(itemSubtotal);

            // Reduce stock via InventoryService so it's logged consistently
            // with every other stock change in the system
            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setProductId(product.getId());
            adjustment.setType(InventoryTransactionType.STOCK_OUT);
            adjustment.setQuantity(quantity);
            adjustment.setReason("Sale " + sale.getInvoiceNumber());

            inventoryService.adjustStock(adjustment);
        }

        BigDecimal discount = request.getDiscount();

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be negative"
            );
        }

        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than subtotal"
            );
        }

        BigDecimal total = subtotal.subtract(discount);

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(total);
        sale.setPaymentMethod(request.getPaymentMethod());

        return saleRepository.save(sale);
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();
    }
}