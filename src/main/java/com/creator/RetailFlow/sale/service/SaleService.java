package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.entity.InventoryTransactionType;
import com.creator.RetailFlow.inventory.service.InventoryService;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.sale.dto.CheckoutItemRequest;
import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.entity.DiscountType;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.entity.SaleItem;
import com.creator.RetailFlow.sale.repository.SaleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Value("${discount.max-amount:5000.00}")
    private BigDecimal maxDiscountAmount = new BigDecimal("5000.00");

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
            throw new IllegalArgumentException(
                    "Payment method is required"
            );
        }

        if (request.getDiscountType() == null) {
            throw new IllegalArgumentException(
                    "Discount type is required"
            );
        }

        BigDecimal discountValue = request.getDiscount() == null
                ? BigDecimal.ZERO
                : request.getDiscount();

        if (discountValue.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be negative"
            );
        }

        // Aggregate duplicate product lines and validate quantities up
        // front, before locking or touching anything.
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();

        for (CheckoutItemRequest itemRequest : request.getItems()) {

            if (itemRequest == null
                    || itemRequest.getProductId() == null
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
                    itemRequest.getProductId(),
                    requestedQuantity,
                    Integer::sum
            );
        }

        // Lock products in a consistent sorted order across all
        // checkouts, so two concurrent carts touching the same
        // products cannot deadlock waiting on each other's locks.
        List<Long> sortedProductIds = quantityByProductId.keySet()
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        Sale sale = new Sale();
        sale.setInvoiceNumber(generateInvoiceNumber());

        BigDecimal subtotal = BigDecimal.ZERO;

        Map<Long, Product> lockedProducts = new LinkedHashMap<>();
        Map<Long, BigDecimal> subtotalByProductId = new LinkedHashMap<>();

        // Pass 1:
        // Lock every product, verify stock and calculate subtotals.
        //
        // No stock is mutated yet. If anything fails here, nothing
        // has changed.
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
                                + ". Available stock: "
                                + availableStock
                );
            }

            BigDecimal itemSubtotal = product.getSellingPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            lockedProducts.put(productId, product);
            subtotalByProductId.put(productId, itemSubtotal);

            subtotal = subtotal.add(itemSubtotal);
        }

        /*
         * Calculate the actual discount only after the real subtotal
         * has been calculated from database product prices.
         *
         * The frontend does not control the final discount amount.
         */
        BigDecimal discount = calculateDiscount(
                subtotal,
                discountValue,
                request.getDiscountType()
        );

        /*
         * Pass 2:
         * Everything has been validated.
         * Now build sale items and reduce stock.
         */
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

            StockAdjustmentRequest adjustment =
                    new StockAdjustmentRequest();

            adjustment.setProductId(productId);
            adjustment.setType(InventoryTransactionType.STOCK_OUT);
            adjustment.setQuantity(quantity);
            adjustment.setReason(
                    "Sale " + sale.getInvoiceNumber()
            );

            inventoryService.adjustStock(adjustment);
        }

        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTotal(subtotal.subtract(discount));
        sale.setPaymentMethod(request.getPaymentMethod());

        return saleRepository.save(sale);
    }

    private BigDecimal calculateDiscount(
            BigDecimal subtotal,
            BigDecimal discountValue,
            DiscountType discountType
    ) {

        if (discountValue == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (discountType == null) {
            throw new IllegalArgumentException(
                    "Discount type is required"
            );
        }

        if (discountValue.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be negative"
            );
        }

        BigDecimal calculatedDiscount;

        switch (discountType) {

            case FIXED -> {
                calculatedDiscount = discountValue;
            }

            case PERCENTAGE -> {

                if (discountValue.compareTo(
                        BigDecimal.valueOf(100)
                ) > 0) {

                    throw new IllegalArgumentException(
                            "Percentage discount cannot exceed 100%"
                    );
                }

                calculatedDiscount = subtotal
                        .multiply(discountValue)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported discount type"
            );
        }

        /*
         * A discount can never make the total negative.
         */
        if (calculatedDiscount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than subtotal"
            );
        }

        /*
         * Enforce the configured maximum discount amount.
         */
        if (calculatedDiscount.compareTo(maxDiscountAmount) > 0) {
            throw new IllegalArgumentException(
                    "Discount exceeds the maximum allowed amount"
            );
        }

        return calculatedDiscount.setScale(
                2,
                RoundingMode.HALF_UP
        );
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
                        "Sale not found with invoice number: "
                                + invoiceNumber
                ));
    }

    public List<Sale> getSalesBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {

        return saleRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(
                        start,
                        end
                );
    }

    // Used by the Invoices page so any logged-in user
    // (including cashiers) can find a past invoice to reprint.
    // Capped at 20 results.
    public List<Sale> lookupSales(String query) {

        String text = query == null
                ? ""
                : query.trim();

        return saleRepository
                .findTop20ByInvoiceNumberContainingIgnoreCaseOrderByCreatedAtDesc(
                        text
                );
    }

    @Transactional
    public Sale voidSale(Long id) {

        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sale not found with id: " + id
                ));

        if (sale.isVoided()) {
            throw new IllegalArgumentException(
                    "Sale is already voided"
            );
        }

        for (SaleItem item : sale.getItems()) {

            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();

            adjustment.setProductId(
                    item.getProduct().getId()
            );

            adjustment.setType(
                    InventoryTransactionType.STOCK_IN
            );

            adjustment.setQuantity(
                    item.getQuantity()
            );

            adjustment.setReason(
                    "Void of sale "
                            + sale.getInvoiceNumber()
            );

            inventoryService.adjustStock(adjustment);
        }

        sale.setVoided(true);
        return saleRepository.save(sale);
    }

    private String generateInvoiceNumber() {

        return "INV-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }
}