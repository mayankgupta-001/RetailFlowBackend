package com.creator.RetailFlow.sale.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/*
 * Running serial for invoice numbers.
 *
 * Exactly one row exists (id = 1, enforced by a CHECK constraint). It
 * stores the financial year it belongs to, so the series can restart on
 * April 1 without a yearly migration.
 */
@Entity
@Table(name = "invoice_counter")
public class InvoiceCounter {

    /** The only row this table may hold. */
    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "financial_year", nullable = false, length = 7)
    private String financialYear;

    @Column(name = "last_number", nullable = false)
    private long lastNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public long getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(long lastNumber) {
        this.lastNumber = lastNumber;
    }
}
