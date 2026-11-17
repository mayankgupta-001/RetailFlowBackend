package com.creator.RetailFlow.discount.service;

import com.creator.RetailFlow.discount.dto.DiscountRequest;
import com.creator.RetailFlow.discount.entity.Discount;
import com.creator.RetailFlow.discount.repository.DiscountRepository;
import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.sale.entity.DiscountType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

    private static final LocalDateTime NOW = LocalDateTime.now();

    // ---------- Creation ----------

    @Test
    void createValidFixedDiscount() {

        when(discountRepository.findOverlappingActive(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = discountService.create(request(
                "Diwali Sale",
                DiscountType.FIXED,
                "25.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        ));

        assertThat(result.getName()).isEqualTo("Diwali Sale");
        assertThat(result.getType()).isEqualTo(DiscountType.FIXED);
        assertThat(result.getValue()).isEqualByComparingTo("25.00");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void createValidPercentageDiscount() {

        when(discountRepository.findOverlappingActive(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = discountService.create(request(
                "Month End",
                DiscountType.PERCENTAGE,
                "10.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        ));

        assertThat(result.getType()).isEqualTo(DiscountType.PERCENTAGE);
        assertThat(result.getValue()).isEqualByComparingTo("10.00");
    }

    @Test
    void createRejectsNegativeValue() {

        assertThatThrownBy(() -> discountService.create(request(
                "Bad",
                DiscountType.FIXED,
                "-1.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount value cannot be negative");

        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void createRejectsPercentageGreaterThan100() {

        assertThatThrownBy(() -> discountService.create(request(
                "Bad",
                DiscountType.PERCENTAGE,
                "100.01",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Percentage discount cannot exceed 100%");

        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void createRejectsEndBeforeStart() {

        assertThatThrownBy(() -> discountService.create(request(
                "Backwards",
                DiscountType.FIXED,
                "25.00",
                NOW.plusDays(2),
                NOW.plusDays(1),
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount end date must not be before start date");

        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void createRejectsOverlapWithActiveDiscount() {

        Discount existing = discount(
                9L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );

        when(discountRepository.findOverlappingActive(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of(existing));

        assertThatThrownBy(() -> discountService.create(request(
                "Diwali Sale 2",
                DiscountType.FIXED,
                "10.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount schedule overlaps with active discount: Diwali Sale"
                );

        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void createAllowsOverlapWhenCreatedDisabled() {

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = discountService.create(request(
                "Future Sale",
                DiscountType.FIXED,
                "10.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                false
        ));

        assertThat(result.isActive()).isFalse();

        // Disabled discounts cannot conflict, so no overlap query runs.
        verify(discountRepository, never())
                .findOverlappingActive(anyLong(), any(), any());
    }

    // ---------- Update ----------

    @Test
    void updateExcludesSelfFromOverlapCheck() {

        Discount existing = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );
        existing.setStartAt(NOW.minusDays(1));
        existing.setEndAt(NOW.plusDays(1));

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(existing));

        when(discountRepository.findOverlappingActive(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        discountService.update(5L, request(
                "Diwali Sale",
                DiscountType.FIXED,
                "30.00",
                NOW.minusDays(1),
                NOW.plusDays(1),
                true
        ));

        // The excluded ID must be the discount being edited.
        verify(discountRepository).findOverlappingActive(
                eq(5L), any(LocalDateTime.class), any(LocalDateTime.class)
        );
    }

    // ---------- Status ----------

    @Test
    void patchStatusEnablingRejectsOverlap() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                false
        );
        discount.setStartAt(NOW.minusDays(1));
        discount.setEndAt(NOW.plusDays(1));

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        Discount conflicting = discount(
                9L,
                "Other Sale",
                DiscountType.FIXED,
                true
        );

        when(discountRepository.findOverlappingActive(
                anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of(conflicting));

        assertThatThrownBy(() -> discountService.patchStatus(5L, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount schedule overlaps with active discount: Other Sale"
                );

        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void patchStatusDisablingAlwaysAllowed() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = discountService.patchStatus(5L, false);

        assertThat(result.isActive()).isFalse();

        verify(discountRepository, never())
                .findOverlappingActive(anyLong(), any(), any());
    }

    // ---------- Checkout hook ----------

    @Test
    void getApplicableDiscountReturnsActiveWithinSchedule() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );
        discount.setStartAt(NOW.minusDays(1));
        discount.setEndAt(NOW.plusDays(1));

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        Discount result = discountService.getApplicableDiscount(5L);

        assertThat(result).isSameAs(discount);
    }

    @Test
    void getApplicableDiscountRejectsDisabled() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                false
        );

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.getApplicableDiscount(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is disabled: Diwali Sale");
    }

    @Test
    void getApplicableDiscountRejectsFutureDiscount() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );
        discount.setStartAt(NOW.plusDays(1));
        discount.setEndAt(NOW.plusDays(2));

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.getApplicableDiscount(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount is not active yet");
    }

    @Test
    void getApplicableDiscountRejectsExpiredDiscount() {

        Discount discount = discount(
                5L,
                "Diwali Sale",
                DiscountType.FIXED,
                true
        );
        discount.setStartAt(NOW.minusDays(2));
        discount.setEndAt(NOW.minusDays(1));

        when(discountRepository.findById(5L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.getApplicableDiscount(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount has expired");
    }

    @Test
    void getApplicableDiscountRejectsUnknownId() {

        when(discountRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.getApplicableDiscount(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Discount not found with id: 999");
    }

    // ---------- Helpers ----------

    private DiscountRequest request(
            String name,
            DiscountType type,
            String value,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active
    ) {
        DiscountRequest request = new DiscountRequest();
        request.setName(name);
        request.setType(type);
        request.setValue(new BigDecimal(value));
        request.setStartAt(startAt);
        request.setEndAt(endAt);
        request.setActive(active);
        return request;
    }

    private Discount discount(
            Long id,
            String name,
            DiscountType type,
            boolean active
    ) {
        Discount discount = new Discount();
        discount.setName(name);
        discount.setType(type);
        discount.setValue(new BigDecimal("25.00"));
        discount.setStartAt(NOW.minusDays(1));
        discount.setEndAt(NOW.plusDays(1));
        discount.setActive(active);
        return discount;
    }
}
