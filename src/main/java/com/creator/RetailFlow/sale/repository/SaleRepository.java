package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByInvoiceNumber(String invoiceNumber);

    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end
    );
}