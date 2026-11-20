package com.creator.RetailFlow.tax;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Inclusive-MRP GST math (issue #21).
 *
 * Prices already include tax, so a line splits as:
 *
 *   net        = gross - apportioned discount
 *   taxable    = net * 100 / (100 + rate)
 *   tax        = net - taxable
 *   cgst, sgst = halves of tax
 *
 * Computing tax as (net - taxable) rather than (taxable * rate) makes
 * "taxable + tax == net" hold exactly, so the invoice totals reconcile
 * without rounding drift.
 *
 * The sale-level discount is apportioned across lines with the
 * largest-remainder method (in paise, integer arithmetic), so the
 * per-line discounts always sum to the exact sale discount - either
 * rounding up or down would leave a stray paisa on multi-line carts.
 */
public final class GstCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    private static final int MONEY_SCALE = 2;

    private GstCalculator() {
    }

    /** A single cart line: tax-inclusive gross amount and its GST rate. */
    public record Line(BigDecimal gross, BigDecimal taxRate) {
    }

    /** Tax split for one line. All amounts are in the same currency. */
    public record LineTax(
            BigDecimal discount,
            BigDecimal netAmount,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount
    ) {
    }

    /** Invoice-level totals plus the per-line breakdown. */
    public record Breakdown(
            List<LineTax> lines,
            BigDecimal taxableAmount,
            BigDecimal taxAmount,
            BigDecimal cgstAmount,
            BigDecimal sgstAmount
    ) {
    }

    public static Breakdown calculate(BigDecimal discount, List<Line> lines) {

        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one line is required to calculate tax"
            );
        }

        BigDecimal totalDiscount = discount == null
                ? money(BigDecimal.ZERO)
                : money(discount);

        if (totalDiscount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be negative"
            );
        }

        long[] grossPaise = new long[lines.size()];
        long subtotalPaise = 0;

        for (int i = 0; i < lines.size(); i++) {

            BigDecimal gross = money(lines.get(i).gross());

            if (gross.signum() < 0) {
                throw new IllegalArgumentException(
                        "Line amount cannot be negative"
                );
            }

            grossPaise[i] = toPaise(gross);
            subtotalPaise = Math.addExact(subtotalPaise, grossPaise[i]);
        }

        long discountPaise = toPaise(totalDiscount);

        if (discountPaise > subtotalPaise) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than subtotal"
            );
        }

        long[] lineDiscountPaise = apportion(
                discountPaise,
                grossPaise,
                subtotalPaise
        );

        List<LineTax> lineTaxes = new ArrayList<>(lines.size());

        BigDecimal taxableTotal = zero();
        BigDecimal taxTotal = zero();
        BigDecimal cgstTotal = zero();
        BigDecimal sgstTotal = zero();

        for (int i = 0; i < lines.size(); i++) {

            BigDecimal gross = BigDecimal.valueOf(grossPaise[i], MONEY_SCALE);
            BigDecimal lineDiscount = BigDecimal.valueOf(
                    lineDiscountPaise[i],
                    MONEY_SCALE
            );
            BigDecimal net = gross.subtract(lineDiscount);

            BigDecimal rate = lines.get(i).taxRate() == null
                    ? BigDecimal.ZERO
                    : lines.get(i).taxRate();

            if (rate.signum() < 0) {
                throw new IllegalArgumentException(
                        "Tax rate cannot be negative"
                );
            }

            if (rate.compareTo(HUNDRED) > 0) {
                throw new IllegalArgumentException(
                        "Tax rate cannot exceed 100%"
                );
            }

            BigDecimal taxable = rate.signum() == 0
                    ? net
                    : net.multiply(HUNDRED).divide(
                            HUNDRED.add(rate),
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );

            // Derive tax from the remainder so taxable + tax == net.
            BigDecimal tax = net.subtract(taxable);

            BigDecimal cgst = tax.divide(
                    TWO,
                    MONEY_SCALE,
                    RoundingMode.HALF_UP
            );
            BigDecimal sgst = tax.subtract(cgst);

            lineTaxes.add(new LineTax(
                    lineDiscount,
                    net,
                    taxable,
                    tax,
                    cgst,
                    sgst
            ));

            taxableTotal = taxableTotal.add(taxable);
            taxTotal = taxTotal.add(tax);
            cgstTotal = cgstTotal.add(cgst);
            sgstTotal = sgstTotal.add(sgst);
        }

        return new Breakdown(
                List.copyOf(lineTaxes),
                taxableTotal,
                taxTotal,
                cgstTotal,
                sgstTotal
        );
    }

    /*
     * Largest-remainder apportionment. Each line gets floor(discount *
     * lineGross / subtotal); the remaining paise go to the lines with
     * the largest fractional remainder, so the sum is exact.
     */
    private static long[] apportion(
            long discountPaise,
            long[] grossPaise,
            long subtotalPaise
    ) {

        int lineCount = grossPaise.length;
        long[] allocated = new long[lineCount];

        if (discountPaise == 0 || subtotalPaise == 0) {
            return allocated;
        }

        BigInteger discount = BigInteger.valueOf(discountPaise);
        BigInteger subtotal = BigInteger.valueOf(subtotalPaise);

        long[] remainders = new long[lineCount];
        long allocatedSum = 0;

        for (int i = 0; i < lineCount; i++) {

            BigInteger[] quotientAndRemainder = discount
                    .multiply(BigInteger.valueOf(grossPaise[i]))
                    .divideAndRemainder(subtotal);

            allocated[i] = quotientAndRemainder[0].longValueExact();
            remainders[i] = quotientAndRemainder[1].longValueExact();
            allocatedSum += allocated[i];
        }

        long leftover = discountPaise - allocatedSum;

        if (leftover > 0) {

            Integer[] order = new Integer[lineCount];

            for (int i = 0; i < lineCount; i++) {
                order[i] = i;
            }

            // Biggest remainder first; lower index wins ties so the
            // result is deterministic.
            Arrays.sort(order, (a, b) -> {
                int byRemainder = Long.compare(remainders[b], remainders[a]);
                return byRemainder != 0
                        ? byRemainder
                        : Integer.compare(a, b);
            });

            for (int k = 0; k < leftover; k++) {
                allocated[order[k % lineCount]]++;
            }
        }

        return allocated;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static long toPaise(BigDecimal amount) {
        return amount
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(MONEY_SCALE)
                .longValueExact();
    }
}
