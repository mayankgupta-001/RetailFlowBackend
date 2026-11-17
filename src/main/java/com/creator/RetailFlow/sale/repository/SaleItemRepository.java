package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SaleItemRepository
        extends JpaRepository<SaleItem, Long> {

    // Loads the items of a whole page of sales in ONE query, so a
    // paginated listing costs 2 queries total instead of 1 per sale.
    List<SaleItem> findBySaleIn(Collection<Sale> sales);
}
