package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository
        extends JpaRepository<SaleItem, Long> {
}