package com.duewise.invoice;

import java.math.BigDecimal;

/** A line item's input values after the service has validated and cleaned them. */
record LineDraft(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal taxRate) {
}
