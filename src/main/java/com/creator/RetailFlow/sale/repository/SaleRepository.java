package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    // Eagerly fetch items with the sale so the controller can map the
    // response without one extra query per sale (N+1).
    @EntityGraph(attributePaths = "items")
    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = "items")
    Optional<Sale> findWithItemsById(Long id);

    // Report listing: pages the sales WITHOUT joining items, so Spring
    // can paginate in the database. Items for the returned page are
    // loaded with one extra query by SaleService (see loadItemsForPage).
    Page<Sale> findByCreatedAtBetween(
            LocalDateTime start, LocalDateTime end, Pageable pageable
    );

    @EntityGraph(attributePaths = "items")
    List<Sale> findTop20ByInvoiceNumberContainingIgnoreCaseOrderByCreatedAtDesc(
            String invoiceNumber
    );
}
