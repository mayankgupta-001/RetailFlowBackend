package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.sale.dto.DiscountRequest;
import com.creator.RetailFlow.sale.dto.DiscountResponse;
import com.creator.RetailFlow.sale.entity.Discount;
import com.creator.RetailFlow.sale.entity.DiscountType;
import com.creator.RetailFlow.sale.repository.DiscountRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

    @Test
    void createValidFixedDiscount() {

        DiscountRequest request = request(
                "Weekend Sale",
                DiscountType.FIXED,
                "500.00",
                "2026-10-30T00:00:00",
                "2026-10-30T23:59:59"
        );

        when(discountRepository.findOverlappingActiveDiscounts(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscountResponse result =
                discountService.create(request);

        assertThat(result.name())
                .isEqualTo("Weekend Sale");

        assertThat(result.type())
                .isEqualTo(DiscountType.FIXED);

        assertThat(result.value())
                .isEqualByComparingTo("500.00");

        assertThat(result.active())
                .isTrue();

        verify(discountRepository)
                .save(any(Discount.class));
    }

    @Test
    void createValidPercentageDiscount() {

        DiscountRequest request = request(
                "Festival Sale",
                DiscountType.PERCENTAGE,
                "20.00",
                "2026-11-01T00:00:00",
                "2026-11-15T23:59:59"
        );

        when(discountRepository.findOverlappingActiveDiscounts(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscountResponse result =
                discountService.create(request);

        assertThat(result.type())
                .isEqualTo(DiscountType.PERCENTAGE);

        assertThat(result.value())
                .isEqualByComparingTo("20.00");
    }

    @Test
    void createRejectsPercentageAbove100() {

        DiscountRequest request = request(
                "Invalid Sale",
                DiscountType.PERCENTAGE,
                "100.01",
                "2026-10-30T00:00:00",
                "2026-10-30T23:59:59"
        );

        assertThatThrownBy(
                () -> discountService.create(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Percentage discount cannot exceed 100%"
                );

        verify(discountRepository, never())
                .save(any(Discount.class));
    }

    @Test
    void createRejectsNegativeValue() {

        DiscountRequest request = request(
                "Invalid Sale",
                DiscountType.FIXED,
                "-10.00",
                "2026-10-30T00:00:00",
                "2026-10-30T23:59:59"
        );

        assertThatThrownBy(
                () -> discountService.create(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount value cannot be negative"
                );

        verify(discountRepository, never())
                .save(any(Discount.class));
    }

    @Test
    void createRejectsInvalidDateRange() {

        DiscountRequest request = request(
                "Invalid Date",
                DiscountType.FIXED,
                "100.00",
                "2026-11-15T00:00:00",
                "2026-11-01T00:00:00"
        );

        assertThatThrownBy(
                () -> discountService.create(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Start date/time cannot be after end date/time"
                );

        verify(discountRepository, never())
                .save(any(Discount.class));
    }

    @Test
    void createRejectsOverlappingActiveDiscount() {

        DiscountRequest request = request(
                "Overlapping Sale",
                DiscountType.FIXED,
                "100.00",
                "2026-10-30T00:00:00",
                "2026-11-05T23:59:59"
        );

        when(discountRepository.findOverlappingActiveDiscounts(
                any(),
                any(),
                any()
        )).thenReturn(List.of(new Discount()));

        assertThatThrownBy(
                () -> discountService.create(request)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Discount schedule overlaps with another active discount"
                );

        verify(discountRepository, never())
                .save(any(Discount.class));
    }

    @Test
    void createAllowsNonOverlappingDiscount() {

        DiscountRequest request = request(
                "Future Sale",
                DiscountType.FIXED,
                "100.00",
                "2026-12-01T00:00:00",
                "2026-12-05T23:59:59"
        );

        when(discountRepository.findOverlappingActiveDiscounts(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscountResponse result =
                discountService.create(request);

        assertThat(result.name())
                .isEqualTo("Future Sale");

        verify(discountRepository)
                .save(any(Discount.class));
    }

    @Test
    void activeDiscountIsReturnedWhenCurrentlyValid() {

        LocalDateTime now = LocalDateTime.now();

        Discount discount = discount(
                1L,
                "Today Sale",
                DiscountType.PERCENTAGE,
                "20.00",
                now.minusHours(1),
                now.plusHours(1),
                true
        );

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        Discount result =
                discountService.getActiveDiscount(1L);

        assertThat(result)
                .isSameAs(discount);
    }

    @Test
    void activeDiscountRejectsDisabledDiscount() {

        LocalDateTime now = LocalDateTime.now();

        Discount discount = discount(
                1L,
                "Disabled Sale",
                DiscountType.FIXED,
                "100.00",
                now.minusHours(1),
                now.plusHours(1),
                false
        );

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(
                () -> discountService.getActiveDiscount(1L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is disabled");
    }

    @Test
    void activeDiscountRejectsFutureDiscount() {

        LocalDateTime now = LocalDateTime.now();

        Discount discount = discount(
                1L,
                "Future Sale",
                DiscountType.FIXED,
                "100.00",
                now.plusHours(1),
                now.plusHours(2),
                true
        );

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(
                () -> discountService.getActiveDiscount(1L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is not currently active");
    }

    @Test
    void activeDiscountRejectsExpiredDiscount() {

        LocalDateTime now = LocalDateTime.now();

        Discount discount = discount(
                1L,
                "Expired Sale",
                DiscountType.FIXED,
                "100.00",
                now.minusHours(2),
                now.minusHours(1),
                true
        );

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        assertThatThrownBy(
                () -> discountService.getActiveDiscount(1L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount is not currently active");
    }

    @Test
    void updateStatusCanDisableDiscount() {

        Discount discount = discount(
                1L,
                "Sale",
                DiscountType.FIXED,
                "100.00",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                true
        );

        when(discountRepository.findById(1L))
                .thenReturn(Optional.of(discount));

        when(discountRepository.save(any(Discount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscountResponse result =
                discountService.updateStatus(1L, false);

        assertThat(result.active())
                .isFalse();

        verify(discountRepository)
                .save(discount);
    }

    private DiscountRequest request(
            String name,
            DiscountType type,
            String value,
            String startAt,
            String endAt
    ) {

        DiscountRequest request = new DiscountRequest();

        request.setName(name);
        request.setType(type);
        request.setValue(new BigDecimal(value));
        request.setStartAt(
                LocalDateTime.parse(startAt)
        );
        request.setEndAt(
                LocalDateTime.parse(endAt)
        );

        return request;
    }

    private Discount discount(
            Long id,
            String name,
            DiscountType type,
            String value,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active
    ) {

        Discount discount = new Discount();

        discount.setName(name);
        discount.setType(type);
        discount.setValue(new BigDecimal(value));
        discount.setStartAt(startAt);
        discount.setEndAt(endAt);
        discount.setActive(active);

        return discount;
    }
}