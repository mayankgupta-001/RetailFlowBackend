package com.creator.RetailFlow.tax;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GstCalculatorTest {

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static GstCalculator.Line line(String gross, String rate) {
        return new GstCalculator.Line(money(gross), money(rate));
    }

    @Test
    void splitsInclusiveAmountIntoExactTaxParts() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("0.00"),
                List.of(line("118.00", "18"))
        );

        GstCalculator.LineTax item = result.lines().get(0);

        assertThat(item.netAmount()).isEqualByComparingTo("118.00");
        assertThat(item.taxableAmount()).isEqualByComparingTo("100.00");
        assertThat(item.taxAmount()).isEqualByComparingTo("18.00");
        assertThat(item.cgstAmount()).isEqualByComparingTo("9.00");
        assertThat(item.sgstAmount()).isEqualByComparingTo("9.00");

        assertThat(result.taxableAmount()).isEqualByComparingTo("100.00");
        assertThat(result.taxAmount()).isEqualByComparingTo("18.00");
    }

    @Test
    void zeroRateKeepsWholeAmountTaxable() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("0.00"),
                List.of(line("99.99", "0"))
        );

        assertThat(result.taxableAmount()).isEqualByComparingTo("99.99");
        assertThat(result.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(result.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.sgstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void splitsFivePercentSlab() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("0.00"),
                List.of(line("105.00", "5"))
        );

        assertThat(result.taxableAmount()).isEqualByComparingTo("100.00");
        assertThat(result.taxAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void discountReducesTaxableValueBeforeTax() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("18.00"),
                List.of(line("118.00", "18"))
        );

        // 118 - 18 = 100 gross; 100 / 1.18 = 84.75 taxable, 15.25 tax.
        assertThat(result.taxableAmount()).isEqualByComparingTo("84.75");
        assertThat(result.taxAmount()).isEqualByComparingTo("15.25");
        assertThat(
                result.taxableAmount().add(result.taxAmount())
        ).isEqualByComparingTo("100.00");
    }

    @Test
    void taxablePlusTaxAlwaysEqualsNetAmount() {

        String[] amounts = {
                "1.00", "0.01", "99.99", "118.00", "999.99", "12345.67"
        };
        String[] rates = {"0", "5", "12", "18", "28"};

        for (String amount : amounts) {
            for (String rate : rates) {

                GstCalculator.Breakdown result = GstCalculator.calculate(
                        money("0.00"),
                        List.of(line(amount, rate))
                );

                GstCalculator.LineTax item = result.lines().get(0);

                assertThat(
                        item.taxableAmount().add(item.taxAmount())
                )
                        .as("taxable + tax for %s at %s%%", amount, rate)
                        .isEqualByComparingTo(item.netAmount());
            }
        }
    }

    @Test
    void cgstPlusSgstAlwaysEqualsTax() {

        for (String amount : new String[]{"0.01", "0.03", "118.00", "99.99"}) {

            GstCalculator.LineTax item = GstCalculator
                    .calculate(
                            money("0.00"),
                            List.of(line(amount, "18"))
                    )
                    .lines()
                    .get(0);

            assertThat(item.cgstAmount().add(item.sgstAmount()))
                    .as("cgst + sgst for %s", amount)
                    .isEqualByComparingTo(item.taxAmount());
        }
    }

    @Test
    void apportionsDiscountExactlyAcrossLines() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("10.00"),
                List.of(
                        line("100.00", "18"),
                        line("50.00", "5"),
                        line("33.33", "12")
                )
        );

        BigDecimal discountSum = result.lines()
                .stream()
                .map(GstCalculator.LineTax::discount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSum = result.lines()
                .stream()
                .map(GstCalculator.LineTax::netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(discountSum).isEqualByComparingTo("10.00");
        assertThat(netSum).isEqualByComparingTo("173.33");

        // Invoice totals must reconcile with the discounted amount.
        assertThat(
                result.taxableAmount().add(result.taxAmount())
        ).isEqualByComparingTo(netSum);
    }

    @Test
    void givesLeftoverPaisaToLargestRemainder() {

        // Three equal lines with a one-paisa discount: exactly one line
        // can receive it, and the sum must stay exact.
        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("0.01"),
                List.of(
                        line("10.00", "18"),
                        line("10.00", "18"),
                        line("10.00", "18")
                )
        );

        BigDecimal discountSum = result.lines()
                .stream()
                .map(GstCalculator.LineTax::discount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(discountSum).isEqualByComparingTo("0.01");

        assertThat(
                result.lines().stream()
                        .filter(item -> item.discount().signum() > 0)
                        .count()
        ).isEqualTo(1);
    }

    @Test
    void fullDiscountProducesZeroTax() {

        GstCalculator.Breakdown result = GstCalculator.calculate(
                money("118.00"),
                List.of(line("118.00", "18"))
        );

        assertThat(result.taxableAmount()).isEqualByComparingTo("0.00");
        assertThat(result.taxAmount()).isEqualByComparingTo("0.00");
        assertThat(result.cgstAmount()).isEqualByComparingTo("0.00");
        assertThat(result.sgstAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsEmptyLines() {

        assertThatThrownBy(() -> GstCalculator.calculate(
                BigDecimal.ZERO,
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("At least one line is required to calculate tax");
    }

    @Test
    void rejectsNegativeDiscount() {

        assertThatThrownBy(() -> GstCalculator.calculate(
                money("-1.00"),
                List.of(line("100.00", "18"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount cannot be negative");
    }

    @Test
    void rejectsDiscountGreaterThanSubtotal() {

        assertThatThrownBy(() -> GstCalculator.calculate(
                money("100.01"),
                List.of(line("100.00", "18"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Discount cannot be greater than subtotal");
    }

    @Test
    void rejectsNegativeRate() {

        assertThatThrownBy(() -> GstCalculator.calculate(
                money("0.00"),
                List.of(line("100.00", "-1"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax rate cannot be negative");
    }

    @Test
    void rejectsRateAboveHundred() {

        assertThatThrownBy(() -> GstCalculator.calculate(
                money("0.00"),
                List.of(line("100.00", "100.01"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tax rate cannot exceed 100%");
    }
}
