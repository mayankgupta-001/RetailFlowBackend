package com.creator.RetailFlow.product.util;

import java.security.SecureRandom;

public class BarcodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    // EAN-13 prefix "2" is internationally reserved for in-store /
    // internal use (e.g. supermarket-generated barcodes for loose or
    // repackaged items) — not for globally registered retail products.
    private static final String IN_STORE_PREFIX = "2";

    public static String generate() {
        StringBuilder digits = new StringBuilder(IN_STORE_PREFIX);

        // Fill 11 more digits randomly (12 digits total before the
        // check digit — EAN-13 is 13 digits including the check digit).
        for (int i = 0; i < 11; i++) {
            digits.append(RANDOM.nextInt(10));
        }

        int checkDigit = calculateCheckDigit(digits.toString());
        digits.append(checkDigit);

        return digits.toString();
    }

    private static int calculateCheckDigit(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < twelveDigits.length(); i++) {
            int digit = Character.getNumericValue(twelveDigits.charAt(i));
            // EAN-13 checksum: odd positions (1st, 3rd, ...) weight 1,
            // even positions weight 3, counting from the left, 1-indexed.
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int remainder = sum % 10;
        return (remainder == 0) ? 0 : 10 - remainder;
    }
}