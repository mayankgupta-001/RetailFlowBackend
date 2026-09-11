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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
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

        if (request.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required");
        }

        BigDecimal discount = request.getDiscount() == null
                ? BigDecimal.ZERO
                : request.getDiscount();

        if (discount.signum() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative");
        }

        // Aggregate duplicate product lines and validate quantities up
        // front, before locking or touching anything.
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
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
            quantityByProductId.merge(
                    itemRequest.getProductId(), requestedQuantity, Integer::sum
            );
        }

        // Lock products in a consistent (sorted) order across all
        // checkouts, so two concurrent carts touching the same two
        // products can never deadlock waiting on each other's locks.
        var sortedProductIds = quantityByProductId.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());

        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        Map<Long, BigDecimal> subtotalByProductId = new LinkedHashMap<>();

        // Pass 1: lock every product, verify stock, compute subtotals.
        // No stock is mutated yet — if anything here fails, nothing has
        // changed and there's nothing to roll back.
        for (Long productId : sortedProductIds) {
            Product product = productRepository
                    .findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + productId
                    ));

            int quantity = quantityByProductId.get(productId);
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

            lockedProducts.put(productId, product);
            subtotalByProductId.put(productId, itemSubtotal);
            subtotal = subtotal.add(itemSubtotal);
        }

        // Discount is validated against the real subtotal before any
        // mutation happens.
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than subtotal"
            );
        }

        // Pass 2: everything is validated — now actually build sale
        // items and reduce stock.
        for (Long productId : sortedProductIds) {
            Product product = lockedProducts.get(productId);
            int quantity = quantityByProductId.get(productId);
            BigDecimal itemSubtotal = subtotalByProductId.get(productId);

            SaleItem saleItem = new SaleItem();
            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setProductName(product.getName());
            saleItem.setBarcode(product.getBarcode());
            saleItem.setPrice(product.getSellingPrice());
            saleItem.setQuantity(quantity);
            saleItem.setSubtotal(itemSubtotal);
            sale.getItems().add(saleItem);

            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setProductId(productId);
            adjustment.setType(InventoryTransactionType.STOCK_OUT);
            adjustment.setQuantity(quantity);
            adjustment.setReason("Sale " + sale.getInvoiceNumber());
            inventoryService.adjustStock(adjustment);
        }

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(subtotal.subtract(discount));
        sale.setPaymentMethod(request.getPaymentMethod());

        return saleRepository.save(sale);
    }

    public Sale getById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale not found with id: " + id
                ));
    }

    public Sale getByInvoiceNumber(String invoiceNumber) {
        return saleRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale not found with invoice number: " + invoiceNumber
                ));
    }

    private String generateInvoiceNumber() {
        // Full UUID — practically collision-proof, unlike an 8-char slice.
        return "INV-" + UUID.randomUUID().toString().toUpperCase();
    }
}