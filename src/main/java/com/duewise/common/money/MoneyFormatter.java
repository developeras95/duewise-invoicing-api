package com.duewise.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats amounts for people to read on PDFs and in emails: the ISO code, then the amount with the
 * currency's decimal places. Rupees use Indian digit grouping (INR 1,25,00,000.00); other currencies
 * use international grouping (USD 12,500,000.00). The code is used rather than a symbol because the
 * standard PDF fonts have no ₹ sign.
 *
 * <p>Java's {@code DecimalFormat} cannot group 3-then-2 digits, so grouping is done here by hand.
 */
public final class MoneyFormatter {

    private MoneyFormatter() {
    }

    public static String format(BigDecimal amount, String currencyCode, int minorUnits) {
        String plain = amount.setScale(minorUnits, RoundingMode.HALF_UP).abs().toPlainString();
        int dot = plain.indexOf('.');
        String whole = dot < 0 ? plain : plain.substring(0, dot);
        String fraction = dot < 0 ? "" : plain.substring(dot);
        String grouped = "INR".equals(currencyCode) ? groupIndian(whole) : groupInternational(whole);
        String sign = amount.signum() < 0 ? "-" : "";
        return currencyCode + " " + sign + grouped + fraction;
    }

    /** 12500000 → 1,25,00,000: the last three digits, then pairs. */
    static String groupIndian(String digits) {
        if (digits.length() <= 3) {
            return digits;
        }
        String lastThree = digits.substring(digits.length() - 3);
        String rest = digits.substring(0, digits.length() - 3);
        StringBuilder out = new StringBuilder();
        int firstPair = rest.length() % 2;
        if (firstPair > 0) {
            out.append(rest, 0, firstPair);
        }
        for (int i = firstPair; i < rest.length(); i += 2) {
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(rest, i, i + 2);
        }
        return out.append(',').append(lastThree).toString();
    }

    /** 12500000 → 12,500,000 */
    static String groupInternational(String digits) {
        StringBuilder out = new StringBuilder();
        int head = digits.length() % 3;
        if (head > 0) {
            out.append(digits, 0, head);
        }
        for (int i = head; i < digits.length(); i += 3) {
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(digits, i, i + 3);
        }
        return out.toString();
    }
}
