package com.creator.RetailFlow.sale.service;

import com.creator.RetailFlow.sale.entity.InvoiceCounter;
import com.creator.RetailFlow.sale.repository.InvoiceCounterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberGeneratorTest {

    @Mock
    private InvoiceCounterRepository counterRepository;

    @InjectMocks
    private InvoiceNumberGenerator generator;

    @Test
    void firstInvoiceOfFinancialYearStartsAtOne() {

        when(counterRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(counter("26-27", 0)));

        assertThat(generator.next(LocalDate.of(2026, 4, 1)))
                .isEqualTo("INV-26-27-0001");
    }

    @Test
    void incrementsTheSerial() {

        when(counterRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(counter("26-27", 41)));

        assertThat(generator.next(LocalDate.of(2026, 8, 15)))
                .isEqualTo("INV-26-27-0042");
    }

    @Test
    void serialIsSavedBack() {

        when(counterRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(counter("26-27", 41)));

        generator.next(LocalDate.of(2026, 8, 15));

        ArgumentCaptor<InvoiceCounter> saved =
                ArgumentCaptor.forClass(InvoiceCounter.class);

        verify(counterRepository).save(saved.capture());

        assertThat(saved.getValue().getLastNumber()).isEqualTo(42);
        assertThat(saved.getValue().getFinancialYear()).isEqualTo("26-27");
    }

    @Test
    void newFinancialYearRestartsTheSeries() {

        // Counter still holds last year's serial; 1 April must not
        // continue the old series.
        when(counterRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(counter("25-26", 1180)));

        assertThat(generator.next(LocalDate.of(2026, 4, 1)))
                .isEqualTo("INV-26-27-0001");
    }

    @Test
    void marchStillBelongsToThePreviousFinancialYear() {

        assertThat(InvoiceNumberGenerator.financialYearOf(
                LocalDate.of(2027, 3, 31)
        )).isEqualTo("26-27");

        assertThat(InvoiceNumberGenerator.financialYearOf(
                LocalDate.of(2027, 4, 1)
        )).isEqualTo("27-28");
    }

    @Test
    void createsTheCounterWhenTheTableIsEmpty() {

        when(counterRepository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());

        assertThat(generator.next(LocalDate.of(2026, 5, 10)))
                .isEqualTo("INV-26-27-0001");

        ArgumentCaptor<InvoiceCounter> saved =
                ArgumentCaptor.forClass(InvoiceCounter.class);

        verify(counterRepository).save(saved.capture());

        assertThat(saved.getValue().getId())
                .isEqualTo(InvoiceCounter.SINGLETON_ID);
        assertThat(saved.getValue().getLastNumber()).isEqualTo(1);
    }

    @Test
    void numberStaysWithinTheSixteenCharacterLimit() {

        // The largest serial that still fits: INV-26-27-999999
        String number = InvoiceNumberGenerator.format("26-27", 999_999);

        assertThat(number).isEqualTo("INV-26-27-999999");
        assertThat(number).hasSize(16);
    }

    @Test
    void serialsAreZeroPadded() {

        assertThat(InvoiceNumberGenerator.format("26-27", 7))
                .isEqualTo("INV-26-27-0007");

        assertThat(InvoiceNumberGenerator.format("26-27", 12_345))
                .isEqualTo("INV-26-27-12345");
    }

    @Test
    void numbersAreUrlSafe() {

        // getSaleByInvoiceNumber puts this in a path segment.
        String number = InvoiceNumberGenerator.format("26-27", 1);

        assertThat(number).doesNotContain("/");
        assertThat(number).matches("[A-Z0-9-]+");
    }

    private static InvoiceCounter counter(
            String financialYear,
            long lastNumber
    ) {
        InvoiceCounter counter = new InvoiceCounter();

        counter.setId(InvoiceCounter.SINGLETON_ID);
        counter.setFinancialYear(financialYear);
        counter.setLastNumber(lastNumber);

        return counter;
    }
}
