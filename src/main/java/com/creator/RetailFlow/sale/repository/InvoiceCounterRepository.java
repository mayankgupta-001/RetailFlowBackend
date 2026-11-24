package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.sale.entity.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InvoiceCounterRepository
        extends JpaRepository<InvoiceCounter, Long> {

    /*
     * SELECT ... FOR UPDATE on the single counter row.
     *
     * The lock is held until the checkout transaction commits, which is
     * what stops two concurrent cashiers from reading the same
     * last_number and printing the same invoice number.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceCounter> findFirstByOrderByIdAsc();
}
