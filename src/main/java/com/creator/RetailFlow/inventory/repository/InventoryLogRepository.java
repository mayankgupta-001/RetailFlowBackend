package com.creator.RetailFlow.inventory.repository;

import com.creator.RetailFlow.inventory.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLogRepository
        extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId);
}