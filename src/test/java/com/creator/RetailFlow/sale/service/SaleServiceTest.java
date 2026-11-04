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
import com.creator.RetailFlow.sale.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.creator.RetailFlow.sale.entity.Discount;
import com.creator.RetailFlow.sale.repository.DiscountRepository;

import java.time.LocalDateTime;

import java.math.BigDecimal;
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
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private DiscountRepository discountRepository;

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
                "-1.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount cannot be negative");

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
    void checkoutAppliesActiveScheduledDiscount() {

        Product product = product(
                "Coffee",
                "8900001",
                "100.00",
                10
        );

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        Discount discount = new Discount();

        LocalDateTime now = LocalDateTime.now();

        discount.setName("20% Weekend Sale");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(new BigDecimal("20.00"));
        discount.setStartAt(now.minusHours(1));
        discount.setEndAt(now.plusHours(1));
        discount.setActive(true);

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        when(saleRepository.save(any(Sale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CheckoutRequest request = request(
                1L,
                2,
                "9999.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        request.setDiscountId(1L);

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");

        assertThat(result.getDiscount())
                .isEqualByComparingTo("40.00");

        assertThat(result.getTotal())
                .isEqualByComparingTo("160.00");

        verify(discountRepository)
                .findById(1L);

        verify(inventoryService)
                .adjustStock(any(StockAdjustmentRequest.class));

        verify(saleRepository)
                .save(any(Sale.class));
    }

    @Test
    void checkoutRejectsDisabledScheduledDiscount() {

        Discount discount = new Discount();

        LocalDateTime now = LocalDateTime.now();

        discount.setName("Disabled Sale");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(new BigDecimal("20.00"));
        discount.setStartAt(now.minusHours(1));
        discount.setEndAt(now.plusHours(1));
        discount.setActive(false);

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        CheckoutRequest request = request(
                1L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        request.setDiscountId(1L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is disabled");

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsFutureScheduledDiscount() {

        Discount discount = new Discount();

        LocalDateTime now = LocalDateTime.now();

        discount.setName("Future Sale");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(new BigDecimal("20.00"));
        discount.setStartAt(now.plusHours(1));
        discount.setEndAt(now.plusHours(2));
        discount.setActive(true);

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        CheckoutRequest request = request(
                1L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        request.setDiscountId(1L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is not currently active");

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsExpiredScheduledDiscount() {

        Discount discount = new Discount();

        LocalDateTime now = LocalDateTime.now();

        discount.setName("Expired Sale");
        discount.setType(DiscountType.PERCENTAGE);
        discount.setValue(new BigDecimal("20.00"));
        discount.setStartAt(now.minusHours(2));
        discount.setEndAt(now.minusHours(1));
        discount.setActive(true);

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        CheckoutRequest request = request(
                1L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        request.setDiscountId(1L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is not currently active");

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsNonExistentScheduledDiscount() {

        when(discountRepository.findById(999L))
                .thenReturn(Optional.empty());

        CheckoutRequest request = request(
                1L,
                1,
                "0.00",
                DiscountType.FIXED,
                PaymentMethod.CASH
        );

        request.setDiscountId(999L);

        assertThatThrownBy(
                () -> saleService.checkout(request)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(
                        "Discount not found with id: 999"
                );

        verifyNoSaleOrInventoryChange();
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