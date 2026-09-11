package com.creator.RetailFlow.inventory.service;

import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.inventory.dto.InventoryLogResponse;
import com.creator.RetailFlow.inventory.dto.StockAdjustmentRequest;
import com.creator.RetailFlow.inventory.entity.InventoryLog;
import com.creator.RetailFlow.inventory.entity.InventoryTransactionType;
import com.creator.RetailFlow.inventory.repository.InventoryLogRepository;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public InventoryService(
            ProductRepository productRepository,
            InventoryLogRepository inventoryLogRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    @Transactional
    public void adjustStock(StockAdjustmentRequest request) {

        // 1. Find the product
//        Product product = productRepository.findById(request.getProductId())
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: "
                                        + request.getProductId()
                        )
                );

        // 2. Get current stock
        int previousStock = product.getStock();
        int newStock;

        // 3. Calculate new stock
        if (request.getType() == InventoryTransactionType.STOCK_IN) {

            // Add stock
            newStock = previousStock + request.getQuantity();

        } else if (request.getType() == InventoryTransactionType.STOCK_OUT) {

            // Remove stock
            newStock = previousStock - request.getQuantity();

            // Prevent negative stock
            if (newStock < 0) {
                throw new IllegalArgumentException(
                        "Insufficient stock. Available stock: "
                                + previousStock
                );
            }

        } else if (request.getType()
                == InventoryTransactionType.ADJUSTMENT) {

            // Set stock to the actual physical quantity
            newStock = request.getQuantity();

        } else {

            throw new IllegalArgumentException(
                    "Invalid inventory transaction type"
            );
        }

        // 4. Update product stock
        product.setStock(newStock);

        // 5. Create inventory log
        InventoryLog inventoryLog = new InventoryLog();

        inventoryLog.setProduct(product);
        inventoryLog.setType(request.getType());
        inventoryLog.setQuantity(request.getQuantity());
        inventoryLog.setPreviousStock(previousStock);
        inventoryLog.setNewStock(newStock);
        inventoryLog.setReason(request.getReason());
        inventoryLog.setCreatedAt(LocalDateTime.now());

        // 6. Save product and inventory log
        productRepository.save(product);
        inventoryLogRepository.save(inventoryLog);
    }

    public List<InventoryLogResponse> getProductInventoryHistory(
            Long productId
    ) {

        // Check whether the product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + productId
            );
        }

        // Get inventory logs and convert them to DTO responses
        return inventoryLogRepository
                .findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(log -> new InventoryLogResponse(
                        log.getId(),
                        log.getProduct().getId(),
                        log.getType(),
                        log.getQuantity(),
                        log.getPreviousStock(),
                        log.getNewStock(),
                        log.getReason(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}