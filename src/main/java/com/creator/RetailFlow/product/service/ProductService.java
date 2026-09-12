package com.creator.RetailFlow.product.service;

import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.product.dto.CreateProductRequest;
import com.creator.RetailFlow.product.dto.ProductResponse;
import com.creator.RetailFlow.product.dto.UpdateProductRequest;
import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.product.util.BarcodeGenerator;
import com.creator.RetailFlow.product.util.BarcodeImageGenerator;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    private static final int MAX_BARCODE_GENERATION_ATTEMPTS = 5;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(CreateProductRequest request) {

        String barcode = request.getBarcode();

        if (barcode == null || barcode.isBlank()) {
            barcode = generateUniqueBarcode();
        } else if (productRepository.existsByBarcode(barcode)) {
            throw new IllegalArgumentException("Barcode already exists");
        }

        Product product = new Product();

        product.setName(request.getName());
        product.setBarcode(barcode);
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());
        product.setStock(request.getStock());
        product.setMinStock(request.getMinStock());
        product.setCategory(request.getCategory());

        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    private String generateUniqueBarcode() {
        for (int attempt = 0; attempt < MAX_BARCODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = BarcodeGenerator.generate();
            if (!productRepository.existsByBarcode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Failed to generate a unique barcode after "
                        + MAX_BARCODE_GENERATION_ATTEMPTS + " attempts"
        );
    }

    public byte[] getBarcodeImage(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );
        try {
            return BarcodeImageGenerator.generatePng(product.getBarcode());
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to generate barcode image for product id: " + id, e
            );
        }
    }

    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        return mapToResponse(product);
    }

    public ProductResponse getProductByBarcode(String barcode) {

        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with barcode: " + barcode
                        )
                );

        return mapToResponse(product);
    }

    public ProductResponse updateProduct(
            Long id,
            UpdateProductRequest request
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        product.setName(request.getName());
        product.setSellingPrice(request.getSellingPrice());
        product.setCostPrice(request.getCostPrice());
        product.setMinStock(request.getMinStock());
        product.setCategory(request.getCategory());

        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBarcode(),
                product.getSellingPrice(),
                product.getCostPrice(),
                product.getStock(),
                product.getMinStock(),
                product.getCategory(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}