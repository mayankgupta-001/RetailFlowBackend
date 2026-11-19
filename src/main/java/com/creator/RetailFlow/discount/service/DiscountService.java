package com.creator.RetailFlow.discount.service;

import com.creator.RetailFlow.discount.dto.DiscountRequest;
import com.creator.RetailFlow.discount.entity.Discount;
import com.creator.RetailFlow.discount.repository.DiscountRepository;
import com.creator.RetailFlow.exception.ResourceNotFoundException;
import com.creator.RetailFlow.sale.entity.DiscountType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiscountService {

    private final DiscountRepository discountRepository;

    @Value("${discount.max-amount:5000.00}")
    private BigDecimal maxDiscountAmount = new BigDecimal("5000.00");

    public DiscountService(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    @Transactional
    public Discount create(DiscountRequest request) {

        validateValue(request.getValue(), request.getType());
        validateSchedule(request.getStartAt(), request.getEndAt());

        if (Boolean.TRUE.equals(request.getActive())) {
            ensureNoOverlap(-1L, request.getStartAt(), request.getEndAt());
        }

        Discount discount = new Discount();
        discount.setName(request.getName().trim());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartAt(request.getStartAt());
        discount.setEndAt(request.getEndAt());
        discount.setActive(Boolean.TRUE.equals(request.getActive()));

        return discountRepository.save(discount);
    }

    @Transactional
    public Discount update(Long id, DiscountRequest request) {

        Discount discount = get(id);

        validateValue(request.getValue(), request.getType());
        validateSchedule(request.getStartAt(), request.getEndAt());

        if (Boolean.TRUE.equals(request.getActive())) {
            ensureNoOverlap(id, request.getStartAt(), request.getEndAt());
        }

        discount.setName(request.getName().trim());
        discount.setType(request.getType());
        discount.setValue(request.getValue());
        discount.setStartAt(request.getStartAt());
        discount.setEndAt(request.getEndAt());
        discount.setActive(Boolean.TRUE.equals(request.getActive()));

        return discountRepository.save(discount);
    }

    @Transactional
    public Discount patchStatus(Long id, boolean active) {

        Discount discount = get(id);

        /*
         * Enabling counts as "activation", so the same overlap rule as
         * creation applies. Disabling can never cause a conflict.
         */
        if (active) {
            ensureNoOverlap(id, discount.getStartAt(), discount.getEndAt());
        }

        discount.setActive(active);
        return discountRepository.save(discount);
    }

    @Transactional
    public void delete(Long id) {
        discountRepository.delete(get(id));
    }

    public Discount get(Long id) {

        return discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discount not found with id: " + id
                ));
    }

    public List<Discount> list() {

        return discountRepository.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        );
    }

    /*
     * Checkout hook: resolves a scheduled discount by ID and enforces
     * the applicability rule:
     *
     *   active = true AND startAt <= now AND now <= endAt
     *
     * The client can only reference a discount by ID; it can never
     * dictate the discount value or bypass the schedule.
     */
    public Discount getApplicableDiscount(Long id) {

        Discount discount = get(id);

        if (!discount.isActive()) {
            throw new IllegalArgumentException(
                    "Discount is disabled: " + discount.getName()
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(discount.getStartAt())) {
            throw new IllegalArgumentException(
                    "Discount is not active yet: "
                            + discount.getName()
                            + " (starts at "
                            + discount.getStartAt()
                            + ")"
            );
        }

        if (now.isAfter(discount.getEndAt())) {
            throw new IllegalArgumentException(
                    "Discount has expired: "
                            + discount.getName()
                            + " (ended at "
                            + discount.getEndAt()
                            + ")"
            );
        }

        return discount;
    }

    /*
     * A single active scheduled discount may be applicable at any
     * moment, so overlapping windows among enabled discounts are
     * rejected (issue #15, requirement 8).
     */
    private void ensureNoOverlap(
            Long excludeId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {

        List<Discount> overlaps = discountRepository.findOverlappingActive(
                excludeId,
                startAt,
                endAt
        );

        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException(
                    "Discount schedule overlaps with active discount: "
                            + overlaps.get(0).getName()
            );
        }
    }

    private void validateValue(BigDecimal value, DiscountType type) {

        if (value == null) {
            throw new IllegalArgumentException(
                    "Discount value is required"
            );
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount value cannot be negative"
            );
        }

        if (type == DiscountType.PERCENTAGE
                && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100%"
            );
        }
    }

    private void validateSchedule(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {

        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException(
                    "Discount schedule start and end dates are required"
            );
        }

        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException(
                    "Discount end date must not be before start date"
            );
        }
    }
}
