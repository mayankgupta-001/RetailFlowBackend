package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    @Query("""
            SELECT d
            FROM Discount d
            WHERE d.active = true
              AND d.startAt <= :endAt
              AND d.endAt >= :startAt
              AND (:excludeId IS NULL OR d.id <> :excludeId)
            """)
    List<Discount> findOverlappingActiveDiscounts(
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeId") Long excludeId
    );

    @Query("""
            SELECT d
            FROM Discount d
            WHERE d.active = true
              AND d.startAt <= :now
              AND d.endAt >= :now
            """)
    Optional<Discount> findCurrentlyActive(
            @Param("now") LocalDateTime now
    );
}