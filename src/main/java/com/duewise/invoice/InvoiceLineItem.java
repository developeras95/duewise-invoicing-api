package com.duewise.invoice;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One line on an invoice. Created and changed only through its {@link Invoice}. */
@Entity
@Table(name = "invoice_line_item")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    @Column(name = "line_no", nullable = false, updatable = false)
    private short lineNo;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 15, scale = 3, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate;

    @Column(name = "line_subtotal", precision = 15, scale = 3, nullable = false)
    private BigDecimal lineSubtotal;

    @Column(name = "line_tax", precision = 15, scale = 3, nullable = false)
    private BigDecimal lineTax;

    @Column(name = "line_total", precision = 15, scale = 3, nullable = false)
    private BigDecimal lineTotal;

    protected InvoiceLineItem() {
        // for JPA
    }

    InvoiceLineItem(Invoice invoice, short lineNo, LineDraft draft, InvoiceMath.LineAmounts amounts) {
        this.invoice = invoice;
        this.lineNo = lineNo;
        update(draft, amounts);
    }

    void update(LineDraft draft, InvoiceMath.LineAmounts amounts) {
        this.description = draft.description();
        this.quantity = draft.quantity();
        this.unitPrice = draft.unitPrice();
        this.taxRate = draft.taxRate();
        this.lineSubtotal = amounts.subtotal();
        this.lineTax = amounts.tax();
        this.lineTotal = amounts.total();
    }

    public Long getId() {
        return id;
    }

    public short getLineNo() {
        return lineNo;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getLineSubtotal() {
        return lineSubtotal;
    }

    public BigDecimal getLineTax() {
        return lineTax;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
