package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.service.InventoryService;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.sale.dto.CheckoutItemRequest;
import com.creator.RetailFlow.sale.dto.CheckoutRequest;
import com.creator.RetailFlow.sale.entity.PaymentMethod;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.repository.SaleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private SaleService saleService;

    @Test
    void checkoutCalculatesTotalsAndReducesStock() {
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

        CheckoutRequest request = request(1L, 2, "25.00", PaymentMethod.CASH);

        Sale result = saleService.checkout(request);

        assertThat(result.getSubtotal())
                .isEqualByComparingTo("200.00");
        assertThat(result.getDiscount())
                .isEqualByComparingTo("25.00");
        assertThat(result.getTotal())
                .isEqualByComparingTo("175.00");
        assertThat(result.getItems()).hasSize(1);

        verify(inventoryService)
                .adjustStock(any(StockAdjustmentRequest.class));
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void checkoutRejectsEmptyCart() {
        CheckoutRequest request = new CheckoutRequest();
        request.setItems(List.of());
        request.setDiscount(BigDecimal.ZERO);
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A sale must contain at least one item");

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
                PaymentMethod.CARD
        );

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found with id: 999");

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
                PaymentMethod.CARD
        );

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verifyNoSaleOrInventoryChange();
    }

    @Test
    void checkoutRejectsDiscountGreaterThanSubtotalBeforeStockChange() {
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
                PaymentMethod.UPI
        );

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount cannot be greater than subtotal");

        // This assertion requires SaleService to validate the final discount
        // before calling inventoryService.adjustStock().
        verify(inventoryService, never())
                .adjustStock(any(StockAdjustmentRequest.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }

    @Test
    void checkoutRejectsNegativeDiscount() {
        CheckoutRequest request = request(
                1L,
                1,
                "-1.00",
                PaymentMethod.CASH
        );

        assertThatThrownBy(() -> saleService.checkout(request))
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
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each item must have a valid product ID");

        verify(productRepository, never()).findByIdForUpdate(any());
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
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThatThrownBy(() -> saleService.checkout(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Each item quantity must be greater than 0");

        verify(productRepository, never()).findByIdForUpdate(any());
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
        product.setSellingPrice(new BigDecimal(sellingPrice));
        product.setStock(stock);
        product.setMinStock(0);
        return product;
    }

    private CheckoutRequest request(
            Long productId,
            int quantity,
            String discount,
            PaymentMethod paymentMethod
    ) {
        CheckoutItemRequest item = new CheckoutItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        CheckoutRequest request = new CheckoutRequest();
        request.setItems(List.of(item));
        request.setDiscount(new BigDecimal(discount));
        request.setPaymentMethod(paymentMethod);
        return request;
    }

    private void verifyNoSaleOrInventoryChange() {
        verify(inventoryService, never())
                .adjustStock(any(StockAdjustmentRequest.class));
        verify(saleRepository, never()).save(any(Sale.class));
    }
}