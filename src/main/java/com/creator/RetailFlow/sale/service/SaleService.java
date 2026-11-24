package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.discount.entity.Discount;
import com.creator.RetailFlow.discount.service.DiscountService;
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
import com.creator.RetailFlow.sale.repository.SaleItemRepository;
import com.creator.RetailFlow.sale.repository.SaleRepository;
import com.creator.RetailFlow.tax.GstCalculator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final DiscountService discountService;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Value("${discount.max-amount:5000.00}")
    private BigDecimal maxDiscountAmount = new BigDecimal("5000.00");

    public SaleService(
            SaleRepository saleRepository,
            SaleItemRepository saleItemRepository,
            ProductRepository productRepository,
            InventoryService inventoryService,
            DiscountService discountService,
            InvoiceNumberGenerator invoiceNumberGenerator
    ) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
        this.discountService = discountService;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
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

        /*
         * A scheduled discount and a manual discount are mutually
         * exclusive - no stacking (issue #15, out of scope).
         */
        if (request.getDiscountId() != null
                && discountValue.signum() > 0) {
            throw new IllegalArgumentException(
                    "Use either a manual discount or a scheduled discount, not both"
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
        sale.setInvoiceNumber(invoiceNumberGenerator.next());

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
         *
         * A scheduled discount is referenced by ID and resolved by the
         * backend (value, type and schedule come from the database).
         */
        BigDecimal discount;
        if (request.getDiscountId() != null) {
            Discount scheduled =
                    discountService.getApplicableDiscount(
                            request.getDiscountId()
                    );
            discount = calculateDiscount(
                    subtotal,
                    scheduled.getValue(),
                    scheduled.getType()
            );
        } else {
            discount = calculateDiscount(
                    subtotal,
                    discountValue,
                    request.getDiscountType()
            );
        }

        /*
         * GST (issue #21). Prices are tax-inclusive, so the discount is
         * apportioned across lines (largest remainder, exact) and each
         * line splits into taxable value + tax. This never changes what
         * the customer pays: taxable + tax == subtotal - discount.
         */
        GstCalculator.Breakdown taxBreakdown = GstCalculator.calculate(
                discount,
                sortedProductIds.stream()
                        .map(productId -> new GstCalculator.Line(
                                subtotalByProductId.get(productId),
                                lockedProducts.get(productId).getTaxRate()
                        ))
                        .toList()
        );

        /*
         * Pass 2:
         * Everything has been validated.
         * Now build sale items and reduce stock.
         */
        for (int lineIndex = 0;
             lineIndex < sortedProductIds.size();
             lineIndex++) {

            Long productId = sortedProductIds.get(lineIndex);
            Product product = lockedProducts.get(productId);
            int quantity = quantityByProductId.get(productId);
            BigDecimal itemSubtotal = subtotalByProductId.get(productId);
            GstCalculator.LineTax lineTax =
                    taxBreakdown.lines().get(lineIndex);

            SaleItem saleItem = new SaleItem();

            saleItem.setSale(sale);
            saleItem.setProduct(product);
            saleItem.setProductName(product.getName());
            saleItem.setBarcode(product.getBarcode());
            saleItem.setPrice(product.getSellingPrice());
            saleItem.setQuantity(quantity);
            saleItem.setSubtotal(itemSubtotal);

            // Snapshot the tax data: editing the product later must not
            // change an invoice that has already been issued.
            saleItem.setTaxRate(product.getTaxRate());
            saleItem.setHsnCode(product.getHsnCode());
            saleItem.setTaxableAmount(lineTax.taxableAmount());
            saleItem.setTaxAmount(lineTax.taxAmount());
            saleItem.setCgstAmount(lineTax.cgstAmount());
            saleItem.setSgstAmount(lineTax.sgstAmount());

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
        sale.setTaxableAmount(taxBreakdown.taxableAmount());
        sale.setTaxAmount(taxBreakdown.taxAmount());
        sale.setCgstAmount(taxBreakdown.cgstAmount());
        sale.setSgstAmount(taxBreakdown.sgstAmount());
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

        return saleRepository.findWithItemsById(id)
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

    // Paginated report listing. Pages the sales in the database, then
    // loads every item of the page with ONE extra query, so the whole
    // request costs 2 queries regardless of page size (instead of one
    // query per sale, the classic N+1).
    public Page<Sale> getSalesBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "'from' must not be after 'to'"
            );
        }

        Page<Sale> page = saleRepository.findByCreatedAtBetween(
                start,
                end,
                pageable
        );

        loadItemsForPage(page.getContent());

        return page;
    }

    // Fetches all items belonging to the given sales in a single query
    // and attaches them to their sale, so the response can be mapped
    // after the transaction/session is gone (open-in-view is disabled).
    private void loadItemsForPage(List<Sale> sales) {

        if (sales.isEmpty()) {
            return;
        }

        Map<Long, List<SaleItem>> itemsBySaleId = saleItemRepository
                .findBySaleIn(sales)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getSale().getId()
                ));

        for (Sale sale : sales) {
            sale.setItems(
                    itemsBySaleId.getOrDefault(
                            sale.getId(),
                            List.of()
                    )
            );
        }
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
}