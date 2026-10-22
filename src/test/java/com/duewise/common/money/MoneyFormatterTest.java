package com.duewise.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyFormatterTest {

    @ParameterizedTest(name = "{0} {1} -> {3}")
    @CsvSource(delimiter = '|', value = {
            "12500000.5 | INR | 2 | INR 1,25,00,000.50",
            "125000     | INR | 2 | INR 1,25,000.00",
            "100000     | INR | 2 | INR 1,00,000.00",
            "29501.18   | INR | 2 | INR 29,501.18",
            "999        | INR | 2 | INR 999.00",
            "1499.985   | INR | 2 | INR 1,499.99",
            "12500000   | USD | 2 | USD 12,500,000.00",
            "150000     | JPY | 0 | JPY 150,000",
            "1234.5     | KWD | 3 | KWD 1,234.500",
            "0          | INR | 2 | INR 0.00",
            "-1500      | USD | 2 | USD -1,500.00"
    })
    void formatsWithTheCurrencysDecimalsAndGrouping(String amount, String code, int minorUnits, String expected) {
        assertThat(MoneyFormatter.format(new BigDecimal(amount), code, minorUnits)).isEqualTo(expected);
    }
}
