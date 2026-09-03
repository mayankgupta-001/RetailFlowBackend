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
        if (request == null || request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "A sale must contain at least one item"
            );
        }

        BigDecimal discount = request.getDiscount() == null
                ? BigDecimal.ZERO
                : request.getDiscount();

        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CheckoutItemRequest itemRequest : request.getItems()) {
            if (itemRequest == null || itemRequest.getProductId() == null
                    || itemRequest.getProductId() <= 0) {
                throw new IllegalArgumentException(
                        "Each item must have a valid product ID"
                );
            }

            Integer requestedQuantity = itemRequest.getQuantity();
            if (requestedQuantity == null || requestedQuantity <= 0) {
                throw new IllegalArgumentException(
                        "Each item quantity must be greater than 0"
                );
            }

            Product product = productRepository
                    .findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: "
                                    + itemRequest.getProductId()
                    ));

            int quantity = requestedQuantity;
            int availableStock = product.getStock();
            if (availableStock < quantity) {
                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + product.getName()
                                + ". Available stock: " + availableStock
                );
            }

            BigDecimal itemSubtotal = product.getSellingPrice()
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

            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setProductId(product.getId());
            adjustment.setType(InventoryTransactionType.STOCK_OUT);
            adjustment.setQuantity(quantity);
            adjustment.setReason("Sale " + sale.getInvoiceNumber());
            inventoryService.adjustStock(adjustment);
        }

        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than subtotal"
            );
        }

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(subtotal.subtract(discount));
        sale.setPaymentMethod(request.getPaymentMethod());

        return saleRepository.save(sale);
    }

    private String generateInvoiceNumber() {
        return "INV-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
    }
}