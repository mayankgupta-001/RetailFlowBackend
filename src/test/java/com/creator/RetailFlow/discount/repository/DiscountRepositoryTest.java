package com.creator.RetailFlow.discount.repository;

import com.creator.RetailFlow.discount.entity.Discount;
import com.creator.RetailFlow.sale.entity.DiscountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Guards the discounts table mapping against schema regressions.
 *
 * The original entity mapped the value column to "value", which is a
 * reserved word in H2: the table silently failed to build in the test
 * profile and only surfaced as a logged DDL error. Persisting through
 * the real repository here means any such failure breaks the build.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:discount_repo_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class DiscountRepositoryTest {

    @Autowired
    private DiscountRepository discountRepository;

    private Discount discount(
            String name,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean active
    ) {
        Discount discount = new Discount();
        discount.setName(name);
        discount.setType(DiscountType.FIXED);
        discount.setValue(new BigDecimal("25.00"));
        discount.setStartAt(startAt);
        discount.setEndAt(endAt);
        discount.setActive(active);
        return discount;
    }

    @Test
    void savesAndReadsDiscountWithScheduledWindow() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 19, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 19, 21, 0);

        Discount saved = discountRepository.saveAndFlush(
                discount("Diwali Sale", start, end, true)
        );

        Discount reloaded = discountRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getName()).isEqualTo("Diwali Sale");
        assertThat(reloaded.getType()).isEqualTo(DiscountType.FIXED);
        assertThat(reloaded.getValue()).isEqualByComparingTo("25.00");
        assertThat(reloaded.getStartAt()).isEqualTo(start);
        assertThat(reloaded.getEndAt()).isEqualTo(end);
        assertThat(reloaded.isActive()).isTrue();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void overlappingWindowQueryFindsEnabledDiscountsOnly() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);

        discountRepository.saveAndFlush(discount(
                "Running",
                now.minusHours(1),
                now.plusHours(1),
                true
        ));
        discountRepository.saveAndFlush(discount(
                "Disabled",
                now.minusHours(1),
                now.plusHours(1),
                false
        ));

        List<Discount> overlaps = discountRepository.findOverlappingActive(
                -1L,
                now.minusMinutes(30),
                now.plusMinutes(30)
        );

        assertThat(overlaps)
                .extracting(Discount::getName)
                .containsExactly("Running");
    }

    @Test
    void nonOverlappingWindowQueryReturnsNothing() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);

        discountRepository.saveAndFlush(discount(
                "Earlier",
                now.minusHours(4),
                now.minusHours(2),
                true
        ));

        List<Discount> overlaps = discountRepository.findOverlappingActive(
                -1L,
                now.minusHours(1),
                now.plusHours(1)
        );

        assertThat(overlaps).isEmpty();
    }
}
