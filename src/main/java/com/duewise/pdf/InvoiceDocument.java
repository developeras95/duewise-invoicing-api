package com.duewise.pdf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.duewise.client.Address;
import com.duewise.client.Client;
import com.duewise.common.money.MoneyFormatter;
import com.duewise.config.BusinessProperties;
import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceLineItem;
import com.duewise.invoice.InvoiceStatus;

/**
 * An invoice already formatted for people to read: every amount, date and address is a finished
 * string. The PDF template and the reminder emails both print from this, so they always agree.
 */
public record InvoiceDocument(
        Long id,
        String invoiceNumber,
        String status,
        boolean draft,
        boolean paid,
        boolean voided,
        String businessName,
        List<String> businessAddress,
        String businessEmail,
        String businessTaxId,
        String clientName,
        List<String> clientAddress,
        String clientTaxId,
        String clientEmail,
        String issueDate,
        String dueDate,
        String currencyCode,
        List<Line> lines,
        String subtotal,
        String taxTotal,
        String total,
        String amountPaid,
        String balanceDue,
        String notes,
        boolean overdue,
        long daysOverdue) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);
    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    public record Line(int lineNo, String description, String quantity, String unitPrice, String taxRate, String lineTotal) {
    }

    public static InvoiceDocument from(Invoice invoice, BusinessProperties business, LocalDate today) {
        String code = invoice.getCurrency().getCode();
        int minorUnits = invoice.getCurrency().getMinorUnits();
        Client client = invoice.getClient();

        List<Line> lines = new ArrayList<>();
        for (InvoiceLineItem item : invoice.getLines()) {
            lines.add(new Line(
                    item.getLineNo(),
                    item.getDescription(),
                    plain(item.getQuantity()),
                    MoneyFormatter.format(item.getUnitPrice(), code, minorUnits),
                    plain(item.getTaxRate()) + "%",
                    MoneyFormatter.format(item.getLineTotal(), code, minorUnits)));
        }

        boolean overdue = invoice.isOverdue(today);
        return new InvoiceDocument(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus().name(),
                invoice.getStatus() == InvoiceStatus.DRAFT,
                invoice.getStatus() == InvoiceStatus.PAID,
                invoice.getStatus() == InvoiceStatus.VOID,
                business.name(),
                business.addressLines(),
                business.email(),
                business.taxId(),
                client.getName(),
                addressLines(client.getAddress()),
                client.getTaxId(),
                client.reminderEmail(),
                DATE.format(invoice.getIssueDate()),
                DATE.format(invoice.getDueDate()),
                code,
                List.copyOf(lines),
                MoneyFormatter.format(invoice.getSubtotal(), code, minorUnits),
                MoneyFormatter.format(invoice.getTaxTotal(), code, minorUnits),
                MoneyFormatter.format(invoice.getTotal(), code, minorUnits),
                MoneyFormatter.format(invoice.getAmountPaid(), code, minorUnits),
                MoneyFormatter.format(invoice.getBalanceDue(), code, minorUnits),
                invoice.getNotes(),
                overdue,
                overdue ? ChronoUnit.DAYS.between(invoice.getDueDate(), today) : 0);
    }

    /** "INV-2026-00042.pdf", or "draft-17.pdf" before the invoice has a number. */
    public String fileName() {
        return (invoiceNumber != null ? invoiceNumber : "draft-" + id) + ".pdf";
    }

    /** What to call the invoice in text: its number, or "Draft 17". */
    public String label() {
        return invoiceNumber != null ? invoiceNumber : "Draft " + id;
    }

    /**
     * Postal lines as printed: "Bengaluru, Karnataka 560001" for the city line and the country's
     * English name ("India") rather than its ISO code. Empty parts are left out.
     */
    static List<String> addressLines(Address address) {
        if (address == null) {
            return List.of();
        }
        String place = joinNonBlank(", ", address.getCity(), address.getState());
        String cityLine = joinNonBlank(" ", place, address.getPostalCode());
        return Stream.of(address.getLine1(), address.getLine2(), cityLine, countryName(address.getCountryCode()))
                .filter(part -> part != null && !part.isBlank())
                .toList();
    }

    /** "IN" → "India". An unrecognised code is printed as it is. */
    static String countryName(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        String code = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!ISO_COUNTRIES.contains(code)) {
            return code;
        }
        String name = Locale.of("", code).getDisplayCountry(Locale.ENGLISH);
        return name.isBlank() ? code : name;
    }

    private static String joinNonBlank(String separator, String... parts) {
        return Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(separator));
    }

    /** 2.000 → "2", 1.500 → "1.5", 18.00 → "18" */
    private static String plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
