package com.creator.RetailFlow.sale.service;

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

    @InjectMocks
    private SaleService saleService;

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