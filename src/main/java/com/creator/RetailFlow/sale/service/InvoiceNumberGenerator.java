package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.sale.entity.InvoiceCounter;
import com.creator.RetailFlow.sale.repository.InvoiceCounterRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/*
 * GST-compliant invoice numbers: INV-<FY>-<serial>, e.g. INV-26-27-0001.
 *
 * Rule 46(b) of the CGST Rules asks for a consecutive serial number, at
 * most 16 characters, using only letters, digits, '-' and '/'.
 *
 * Hyphens are used instead of slashes because the frontend fetches a sale
 * with /api/sales/invoice/{invoiceNumber}: a slash inside the path
 * arrives as %2F, which the servlet container rejects with 400.
 */
@Component
public class InvoiceNumberGenerator {

    private final InvoiceCounterRepository counterRepository;

    public InvoiceNumberGenerator(
            InvoiceCounterRepository counterRepository
    ) {
        this.counterRepository = counterRepository;
    }

    public String next() {
        return next(LocalDate.now());
    }

    public String next(LocalDate date) {

        String financialYear = financialYearOf(date);

        // Row lock: the serial must not repeat or skip when two
        // checkouts commit at the same time. Held until the checkout
        // transaction ends.
        InvoiceCounter counter = counterRepository
                .findFirstByOrderByIdAsc()
                .orElseGet(() -> newCounter(financialYear));

        if (!financialYear.equals(counter.getFinancialYear())) {
            // April 1: start the new year's series at 1.
            counter.setFinancialYear(financialYear);
            counter.setLastNumber(0);
        }

        counter.setLastNumber(counter.getLastNumber() + 1);
        counterRepository.save(counter);

        return format(financialYear, counter.getLastNumber());
    }

    private InvoiceCounter newCounter(String financialYear) {
        InvoiceCounter counter = new InvoiceCounter();
        counter.setId(InvoiceCounter.SINGLETON_ID);
        counter.setFinancialYear(financialYear);
        counter.setLastNumber(0);
        return counter;
    }

    static String format(String financialYear, long serial) {
        // 4 digits minimum; grows to 5 or 6 if a year is ever that busy,
        // which is exactly where the 16-character ceiling lands.
        return "INV-" + financialYear + "-" + String.format("%04d", serial);
    }

    // The Indian financial year runs April 1 to March 31, so January to
    // March still belongs to the year that started last April.
    static String financialYearOf(LocalDate date) {

        int startYear = date.getMonthValue() >= 4
                ? date.getYear()
                : date.getYear() - 1;

        return String.format(
                "%02d-%02d",
                startYear % 100,
                (startYear + 1) % 100
        );
    }
}
