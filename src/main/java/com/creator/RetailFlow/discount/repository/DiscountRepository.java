package com.creator.RetailFlow.discount.repository;

import com.creator.RetailFlow.discount.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    /*
     * Enabled discounts whose configured window overlaps
     * [startAt, endAt]. Overlap is inclusive on both ends so two
     * same-day discounts cannot both become applicable.
     *
     * excludeId lets updates skip the discount being edited; pass -1
     * when creating.
     */
    @Query("""
            SELECT d FROM Discount d
            WHERE d.active = true
              AND d.id <> :excludeId
              AND d.startAt <= :endAt
              AND d.endAt >= :startAt
            """)
    List<Discount> findOverlappingActive(
            @Param("excludeId") Long excludeId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
