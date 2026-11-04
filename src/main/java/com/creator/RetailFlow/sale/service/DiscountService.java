package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.sale.dto.DiscountRequest;
import com.creator.RetailFlow.sale.dto.DiscountResponse;
import com.creator.RetailFlow.sale.entity.Discount;
import com.creator.RetailFlow.sale.entity.DiscountType;
import com.creator.RetailFlow.sale.repository.DiscountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Transactional
    public DiscountResponse create(DiscountRequest request) {

        validateRequest(request);

        validateNoOverlap(
                request.getStartAt(),
                request.getEndAt(),
                null
        );

        Discount discount = new Discount();

        discount.setName(request.getName().trim());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartAt(request.getStartAt());
        discount.setEndAt(request.getEndAt());
        discount.setActive(true);

        return toResponse(
                discountRepository.save(discount)
        );
    }

    @Transactional
    public DiscountResponse update(
            Long id,
            DiscountRequest request
    ) {

        validateRequest(request);

        Discount discount = getEntity(id);

        validateNoOverlap(
                request.getStartAt(),
                request.getEndAt(),
                id
        );

        discount.setName(request.getName().trim());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartAt(request.getStartAt());
        discount.setEndAt(request.getEndAt());

        return toResponse(
                discountRepository.save(discount)
        );
    }

    @Transactional
    public DiscountResponse updateStatus(
            Long id,
            boolean active
    ) {

        Discount discount = getEntity(id);

        if (active) {
            validateNoOverlap(
                    discount.getStartAt(),
                    discount.getEndAt(),
                    id
            );
        }

        discount.setActive(active);

        return toResponse(
                discountRepository.save(discount)
        );
    }

    @Transactional
    public void delete(Long id) {

        Discount discount = getEntity(id);

        discountRepository.delete(discount);
    }

    @Transactional
    public DiscountResponse getById(Long id) {

        return toResponse(getEntity(id));
    }

    @Transactional
    public List<DiscountResponse> getAll() {

        return discountRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Discount getActiveDiscount(Long id) {

        Discount discount = getEntity(id);

        LocalDateTime now = LocalDateTime.now();

        if (!discount.isActive()) {
            throw new IllegalArgumentException(
                    "Discount is disabled"
            );
        }

        if (now.isBefore(discount.getStartAt())
                || now.isAfter(discount.getEndAt())) {

            throw new IllegalArgumentException(
                    "Discount is not currently active"
            );
        }

        return discount;
    }

    private void validateRequest(
            DiscountRequest request
    ) {

        if (request.getType() == null) {
            throw new IllegalArgumentException(
                    "Discount type is required"
            );
        }

        if (request.getValue() == null) {
            throw new IllegalArgumentException(
                    "Discount value is required"
            );
        }

        if (request.getValue().signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount value cannot be negative"
            );
        }

        if (request.getType() == DiscountType.PERCENTAGE
                && request.getValue().compareTo(
                BigDecimal.valueOf(100)
        ) > 0) {

            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100%"
            );
        }

        if (request.getStartAt() == null
                || request.getEndAt() == null) {

            throw new IllegalArgumentException(
                    "Start and end date/time are required"
            );
        }

        if (request.getStartAt().isAfter(request.getEndAt())) {

            throw new IllegalArgumentException(
                    "Start date/time cannot be after end date/time"
            );
        }
    }

    private void validateNoOverlap(
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long excludeId
    ) {

        List<Discount> overlapping =
                discountRepository.findOverlappingActiveDiscounts(
                        startAt,
                        endAt,
                        excludeId
                );

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "Discount schedule overlaps with another active discount"
            );
        }
    }

    private Discount getEntity(Long id) {

        return discountRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Discount not found with id: " + id
                        )
                );
    }

    private DiscountResponse toResponse(
            Discount discount
    ) {

        return new DiscountResponse(
                discount.getId(),
                discount.getName(),
                discount.getType(),
                discount.getValue(),
                discount.getStartAt(),
                discount.getEndAt(),
                discount.isActive(),
                discount.getCreatedAt(),
                discount.getUpdatedAt()
        );
    }
}