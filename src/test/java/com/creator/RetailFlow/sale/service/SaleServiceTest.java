package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.discount.entity.Discount;
import com.creator.RetailFlow.discount.service.DiscountService;
import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.service.InventoryService;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.sale.dto.CheckoutItemRequest;
import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.entity.DiscountType;
import com.creator.RetailFlow.sale.entity.PaymentMethod;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.entity.SaleItem;
import com.creator.RetailFlow.sale.repository.SaleItemRepository;
import com.creator.RetailFlow.sale.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DiscountService discountService;

    @Mock
    private InvoiceNumberGenerator invoiceNumberGenerator;

    @InjectMocks
    private SaleService saleService;

    // Invoice numbering has its own test; here we only need a value so
    // the sale is well formed. Lenient because several tests reject the
    // checkout before a number is ever requested.
    @BeforeEach
    void stubInvoiceNumber() {
        lenient().when(invoiceNumberGenerator.next())
                .thenReturn("INV-26-27-0001");
    }

    @Test
    void checkoutCalculatesFixedDiscountCorrectly() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "25.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("25.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("175.00");

        assertThat(result.getItems())
                .hasSize(1);

        verify(inventoryService)
                .adjustStock(any(StockAdjustmentRequest.class));

        verify(saleRepository)
                .save(any(Sale.class));
    }

    @Test
    void checkoutCalculatesPercentageDiscountCorrectly() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "20.00",
                DiscountType.PERCENTAGE,
                PaymentMethod.CARD
        );

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("40.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("160.00");

        verify(inventoryService)
                .adjustStock(any(StockAdjustmentRequest.class));

        verify(saleRepository)
                .save(any(Sale.class));
    }

    @Test
    void checkoutRejectsPercentageGreaterThan100() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        CheckoutRequest request = request(
                1L,
                1,
                "100.01",
                DiscountType.PERCENTAGE,
                PaymentMethod.CARD
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Percentage discount cannot exceed 100%"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutAllowsExactly100PercentDiscount() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "100.00",
                DiscountType.PERCENTAGE,
                PaymentMethod.CARD
        );

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("200.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("0.00");
    }

    @Test
    void checkoutRejectsFixedDiscountGreaterThanSubtotal() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        CheckoutRequest request = request(
                1L,
                1,
                "101.00",
                DiscountType.FIXED,
                PaymentMethod.UPI
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount cannot be greater than subtotal"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsPercentageDiscountAboveMaximumAmount() {

        Product product = product(
                "Laptop",
                "8900002",
                "50000.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        CheckoutRequest request = request(
                1L,
                1,
                "20.00",
                DiscountType.PERCENTAGE,
                PaymentMethod.CARD
        );

        /*
         * 20% of ₹50,000 = ₹10,000.
         *
         * Maximum configured discount = ₹5,000.
         */
        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount exceeds the maximum allowed amount"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsFixedDiscountAboveMaximumAmount() {

        Product product = product(
                "Laptop",
                "8900002",
                "10000.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        CheckoutRequest request = request(
                1L,
                1,
                "5000.01",
                DiscountType.FIXED,
                PaymentMethod.CARD
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount exceeds the maximum allowed amount"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutAllowsDiscountExactlyAtMaximumAmount() {

        Product product = product(
                "Laptop",
                "8900002",
                "10000.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                1,
                "5000.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("10000.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("5000.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("5000.00");
    }

    @Test
    void checkoutRejectsEmptyCart() {

        CheckoutRequest request = new CheckoutRequest();

        request.setItems(List.of());
        request.setDiscount(BigDecimal.ZERO);
        request.setDiscountType(DiscountType.FIXED);
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "A sale must contain at least one item"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsMissingProduct() {

        when(productRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        CheckoutRequest request = request(
                999L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CARD
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Product not found with id: 999"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsInsufficientStock() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                1
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        CheckoutRequest request = request(
                1L,
                2,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CARD
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Insufficient stock"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsNegativeDiscount() {

        CheckoutRequest request = request(
                1L,
                1,
                "-1.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount cannot be negative"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsInvalidProductId() {

        CheckoutItemRequest item = new CheckoutItemRequest();

        item.setProductId(0L);
        item.setQuantity(1);

        CheckoutRequest request = new CheckoutRequest();

        request.setItems(List.of(item));
        request.setDiscount(BigDecimal.ZERO);
        request.setDiscountType(DiscountType.FIXED);
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Each item must have a valid product ID"
                );

        verify(
                productRepository,
                never()
        ).findByIdForUpdate(any());

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsInvalidQuantity() {

        CheckoutItemRequest item = new CheckoutItemRequest();

        item.setProductId(1L);
        item.setQuantity(0);

        CheckoutRequest request = new CheckoutRequest();

        request.setItems(List.of(item));
        request.setDiscount(BigDecimal.ZERO);
        request.setDiscountType(DiscountType.FIXED);
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Each item quantity must be greater than 0"
                );

        verify(
                productRepository,
                never()
        ).findByIdForUpdate(any());

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutAppliesZeroDiscountCorrectly() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "0.00",
                DiscountType.PERCENTAGE,
                PaymentMethod.CASH
        );

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("0.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("200.00");
    }

    @Test
    void getSalesBetweenRejectsInvertedRange() {

        LocalDateTime start = LocalDateTime.of(2026, 1, 2, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThatThrownBy(
                () -> saleService.getSalesBetween(
                        start, end, PageRequest.of(0, 20)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'from' must not be after 'to'"
                );

        verify(saleRepository, never())
                .findByCreatedAtBetween(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(Pageable.class)
                );
    }

    @Test
    void getSalesBetweenLoadsPageItemsWithOneQuery() {

        Sale sale = new Sale();
        sale.setInvoiceNumber("INV-1");
        ReflectionTestUtils.setField(sale, "id", 1L);

        Page<Sale> page = new PageImpl<>(
                List.of(sale),
                PageRequest.of(0, 20),
                1
        );

        when(saleRepository.findByCreatedAtBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(page);

        SaleItem item = new SaleItem();
        item.setSale(sale);

        when(saleItemRepository.findBySaleIn(any()))
                .thenReturn(List.of(item));

        Page<Sale> result = saleService.getSalesBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);

        assertThat(result.getContent().get(0).getItems())
                .containsExactly(item);

        verify(saleItemRepository)
                .findBySaleIn(any());
    }

    @Test
    void getSalesBetweenSkipsItemQueryForEmptyPage() {

        Page<Sale> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        );

        when(saleRepository.findByCreatedAtBetween(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        Page<Sale> result = saleService.getSalesBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59),
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).isEmpty();

        verify(saleItemRepository, never())
                .findBySaleIn(any());
    }

    @Test
    void checkoutAppliesScheduledFixedDiscount() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(discountService.getApplicableDiscount(5L))
                .thenReturn(scheduledDiscount(
                        "Diwali Sale",
                        DiscountType.FIXED,
                        "25.00"
                ));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );
        request.setDiscountId(5L);

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("25.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("175.00");
    }

    @Test
    void checkoutAppliesScheduledPercentageDiscount() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(discountService.getApplicableDiscount(5L))
                .thenReturn(scheduledDiscount(
                        "Diwali Sale",
                        DiscountType.PERCENTAGE,
                        "20.00"
                ));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );
        request.setDiscountId(5L);

        Sale result = saleService.checkout(request);

        assertThat(result.getDiscount())
                .isEqualByComparingTo("40.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("160.00");
    }

    @Test
    void checkoutRejectsManualAndScheduledDiscountTogether() {

        CheckoutRequest request = request(
                1L,
                2,
                "10.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );
        request.setDiscountId(5L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Use either a manual discount or a scheduled discount, not both"
                );

        verify(discountService, never())
                .getApplicableDiscount(any());

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsDisabledScheduledDiscount() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(discountService.getApplicableDiscount(5L))
                .thenThrow(new IllegalArgumentException(
                        "Discount is disabled: Diwali Sale"
                ));

        CheckoutRequest request = request(
                1L,
                2,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );
        request.setDiscountId(5L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount is disabled: Diwali Sale"
                );

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutSplitsInclusiveTaxOnTaxedProduct() {

        Product product = taxedProduct(
                "Coffee",
                "8900001",
                "118.00",
                10,
                "18",
                "1001"
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.checkout(request(
                1L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        ));

        // The customer still pays ₹118 - only the split changes.
        assertThat(result.getTotal()).isEqualByComparingTo("118.00");
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("100.00");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(result.getCgstAmount()).isEqualByComparingTo("9.00");
        assertThat(result.getSgstAmount()).isEqualByComparingTo("9.00");

        SaleItem item = result.getItems().get(0);

        assertThat(item.getTaxRate()).isEqualByComparingTo("18");
        assertThat(item.getHsnCode()).isEqualTo("1001");
        assertThat(item.getTaxableAmount()).isEqualByComparingTo("100.00");
        assertThat(item.getTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(item.getCgstAmount()).isEqualByComparingTo("9.00");
        assertThat(item.getSgstAmount()).isEqualByComparingTo("9.00");
    }

    @Test
    void checkoutKeepsTotalsForZeroTaxProduct() {

        Product product = taxedProduct(
                "Bread",
                "8900003",
                "40.00",
                10,
                "0",
                null
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.checkout(request(
                1L,
                2,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        ));

        assertThat(result.getTotal()).isEqualByComparingTo("80.00");
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("80.00");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getCgstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.getSgstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void checkoutReducesTaxableValueWhenDiscountApplied() {

        Product product = taxedProduct(
                "Coffee",
                "8900001",
                "118.00",
                10,
                "18",
                "1001"
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Sale result = saleService.checkout(request(
                1L,
                1,
                "18.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        ));

        assertThat(result.getTotal()).isEqualByComparingTo("100.00");
        assertThat(result.getTaxableAmount()).isEqualByComparingTo("84.75");
        assertThat(result.getTaxAmount()).isEqualByComparingTo("15.25");
    }

    @Test
    void checkoutApportionsSaleDiscountAcrossTaxLines() {

        Product first = taxedProduct(
                "Coffee",
                "8900001",
                "118.00",
                10,
                "18",
                "1001"
        );
        Product second = taxedProduct(
                "Tea",
                "8900002",
                "82.00",
                10,
                "18",
                "1002"
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(first));
        when(productRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(second));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = new CheckoutRequest();

        CheckoutItemRequest firstItem = new CheckoutItemRequest();
        firstItem.setProductId(1L);
        firstItem.setQuantity(1);

        CheckoutItemRequest secondItem = new CheckoutItemRequest();
        secondItem.setProductId(2L);
        secondItem.setQuantity(1);

        request.setItems(List.of(firstItem, secondItem));
        request.setDiscount(new BigDecimal("10.00"));
        request.setDiscountType(DiscountType.FIXED);
        request.setPaymentMethod(PaymentMethod.CASH);

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal()).isEqualByComparingTo("200.00");
        assertThat(result.getTotal()).isEqualByComparingTo("190.00");

        // Every line must reconcile, and the apportioned line nets must
        // sum to the discounted sale total (no stray paisa).
        SaleItem firstLine = result.getItems().get(0);
        SaleItem secondLine = result.getItems().get(1);

        assertThat(
                firstLine.getTaxableAmount().add(firstLine.getTaxAmount())
        ).isEqualByComparingTo("112.10");

        assertThat(
                secondLine.getTaxableAmount().add(secondLine.getTaxAmount())
        ).isEqualByComparingTo("77.90");

        BigDecimal lineNetSum = result.getItems()
                .stream()
                .map(item -> item.getTaxableAmount().add(item.getTaxAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lineTaxableSum = result.getItems()
                .stream()
                .map(SaleItem::getTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lineTaxSum = result.getItems()
                .stream()
                .map(SaleItem::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(lineNetSum).isEqualByComparingTo(result.getTotal());
        assertThat(lineTaxableSum)
                .isEqualByComparingTo(result.getTaxableAmount());
        assertThat(lineTaxSum)
                .isEqualByComparingTo(result.getTaxAmount());

        assertThat(
                result.getTaxableAmount().add(result.getTaxAmount())
        ).isEqualByComparingTo("190.00");
    }

    private Product taxedProduct(
            String name,
            String barcode,
            String sellingPrice,
            int stock,
            String taxRate,
            String hsnCode
    ) {
        Product product = product(name, barcode, sellingPrice, stock);
        product.setTaxRate(new BigDecimal(taxRate));
        product.setHsnCode(hsnCode);
        return product;
    }

    private Product product(
            String name,
            String barcode,
            String sellingPrice,
            int stock
    ) {
        Product product = new Product();
        product.setName(name);
        product.setBarcode(barcode);
        product.setSellingPrice(
                new BigDecimal(sellingPrice)
        );
        product.setStock(stock);
        product.setMinStock(0);

        return product;
    }

    private Discount scheduledDiscount(
            String name,
            DiscountType type,
            String value
    ) {
        Discount discount = new Discount();
        discount.setName(name);
        discount.setType(type);
        discount.setValue(new BigDecimal(value));
        discount.setStartAt(LocalDateTime.now().minusDays(1));
        discount.setEndAt(LocalDateTime.now().plusDays(1));
        discount.setActive(true);
        return discount;
    }

    private CheckoutRequest request(
            Long productId,
            int quantity,
            String discount,
            DiscountType discountType,
            PaymentMethod paymentMethod
    ) {

        CheckoutItemRequest item =
                new CheckoutItemRequest();

        item.setProductId(productId);
        item.setQuantity(quantity);

        CheckoutRequest request =
                new CheckoutRequest();

        request.setItems(List.of(item));
        request.setDiscount(
                new BigDecimal(discount)
        );
        request.setDiscountType(discountType);
        request.setPaymentMethod(paymentMethod);

        return request;
    }

    private void verifyNoSaleOrInventoryChange() {

        verify(
                inventoryService,
                never()
        ).adjustStock(
                any(StockAdjustmentRequest.class)
        );

        verify(
                saleRepository,
                never()
        ).save(
                any(Sale.class)
        );
    }
}