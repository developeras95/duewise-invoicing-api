package com.duewise.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class InvoiceMathTest {

    @Test
    void gstLineInRupees() {
        InvoiceMath.LineAmounts line = InvoiceMath.line(bd("2"), bd("12500.50"), bd("18"), 2);

        assertThat(line.subtotal()).isEqualByComparingTo("25001.00");
        assertThat(line.tax()).isEqualByComparingTo("4500.18");
        assertThat(line.total()).isEqualByComparingTo("29501.18");
    }

    @Test
    void roundsHalfUpNotHalfEven() {
        // 1.5 × 0.99 = 1.485 → 1.49 (half-even "banker's rounding" would give 1.48)
        InvoiceMath.LineAmounts line = InvoiceMath.line(bd("1.5"), bd("0.99"), BigDecimal.ZERO, 2);

        assertThat(line.subtotal()).isEqualByComparingTo("1.49");
    }

    @Test
    void yenHasNoDecimalPlaces() {
        // 3 × ¥333 = ¥999; 10% tax = ¥99.9 → ¥100
        InvoiceMath.LineAmounts line = InvoiceMath.line(bd("3"), bd("333"), bd("10"), 0);

        assertThat(line.subtotal()).isEqualByComparingTo("999");
        assertThat(line.tax()).isEqualByComparingTo("100");
        assertThat(line.total()).isEqualByComparingTo("1099");
        assertThat(line.total().scale()).isZero();
    }

    @Test
    void kuwaitiDinarKeepsThreeDecimalPlaces() {
        // 3 × 1.235 = 3.705; 5% tax = 0.18525 → 0.185
        InvoiceMath.LineAmounts line = InvoiceMath.line(bd("3"), bd("1.235"), bd("5"), 3);

        assertThat(line.tax()).isEqualByComparingTo("0.185");
        assertThat(line.total()).isEqualByComparingTo("3.890");
    }

    @Test
    void detectsAmountsWithTooManyDecimalPlaces() {
        assertThat(InvoiceMath.exceedsPrecision(bd("1500.50"), 0)).isTrue();
        assertThat(InvoiceMath.exceedsPrecision(bd("1500.00"), 0)).isFalse();
        assertThat(InvoiceMath.exceedsPrecision(bd("1500"), 0)).isFalse();
        assertThat(InvoiceMath.exceedsPrecision(bd("12.345"), 2)).isTrue();
        assertThat(InvoiceMath.exceedsPrecision(bd("12.340"), 2)).isFalse();
    }

    @Test
    void trimZerosNeverUsesExponentNotation() {
        assertThat(InvoiceMath.trimZeros(bd("10.000")).toString()).isEqualTo("10");
        assertThat(InvoiceMath.trimZeros(bd("1.500")).toString()).isEqualTo("1.5");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
