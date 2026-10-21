package com.duewise.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money arithmetic for invoices. Pure functions with no Spring or database, so every rounding rule
 * can be unit-tested directly.
 *
 * <p>Rules:
 * <ul>
 *   <li>All amounts are {@link BigDecimal}; floating point is never used for money.</li>
 *   <li>Each line is rounded to the currency's decimal places (2 for INR, 0 for JPY, 3 for KWD),
 *       half-up, as on a printed invoice.</li>
 *   <li>Tax is calculated per line on the rounded line subtotal, then rounded.</li>
 *   <li>Invoice totals are the sums of the rounded line values, so the lines always add up to the
 *       total shown.</li>
 * </ul>
 */
final class InvoiceMath {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    /** Largest amount a NUMERIC(15,3) column can hold. */
    static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.999");

    private InvoiceMath() {
    }

    record LineAmounts(BigDecimal subtotal, BigDecimal tax, BigDecimal total) {
    }

    static LineAmounts line(BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRatePercent, int minorUnits) {
        BigDecimal subtotal = round(quantity.multiply(unitPrice), minorUnits);
        BigDecimal tax = round(subtotal.multiply(taxRatePercent).divide(ONE_HUNDRED), minorUnits);
        return new LineAmounts(subtotal, tax, subtotal.add(tax));
    }

    static BigDecimal round(BigDecimal amount, int minorUnits) {
        return amount.setScale(minorUnits, RoundingMode.HALF_UP);
    }

    /**
     * Drops trailing zeros for display (2.000 → 2, 1.500 → 1.5) without switching to exponent
     * notation (10.000 must become 10, not 1E+1).
     */
    static BigDecimal trimZeros(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }

    /** True if the amount has more decimal places than the currency allows, e.g. ¥1500.50. */
    static boolean exceedsPrecision(BigDecimal amount, int minorUnits) {
        return amount.stripTrailingZeros().scale() > minorUnits;
    }
}
