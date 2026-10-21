package com.duewise.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.duewise.client.Client;
import com.duewise.common.error.BadRequestException;
import com.duewise.common.error.BusinessRuleException;
import com.duewise.common.error.ConflictException;
import com.duewise.currency.Currency;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * An invoice and its line items. All status rules live here, so no caller can, for example, edit
 * a sent invoice or void one that has been paid: the entity refuses.
 */
@Entity
@Table(name = "invoice")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null while DRAFT; assigned from the gapless counter when the invoice is sent. */
    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "subtotal", precision = 15, scale = 3, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_total", precision = 15, scale = 3, nullable = false)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "total", precision = 15, scale = 3, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "amount_paid", precision = 15, scale = 3, nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<InvoiceLineItem> lines = new ArrayList<>();

    protected Invoice() {
        // for JPA
    }

    Invoice(Client client, Currency currency, LocalDate issueDate, LocalDate dueDate, String notes) {
        this.status = InvoiceStatus.DRAFT;
        setHeader(client, currency, issueDate, dueDate, notes);
    }

    // ----- Draft editing --------------------------------------------------------------------

    void updateDraft(Client client, Currency currency, LocalDate issueDate, LocalDate dueDate, String notes) {
        ensureEditable();
        setHeader(client, currency, issueDate, dueDate, notes);
        // Always mark the invoice as changed, so the version moves even if only a line changed.
        this.updatedAt = Instant.now();
    }

    /**
     * Replaces the lines with the given ones and recalculates every amount. Existing line rows are
     * updated in place by position, extra rows added and surplus rows removed, which keeps the
     * (invoice_id, line_no) unique constraint happy during the flush.
     */
    void replaceLines(List<LineDraft> drafts, int minorUnits) {
        ensureEditable();
        for (int i = 0; i < drafts.size(); i++) {
            LineDraft draft = drafts.get(i);
            InvoiceMath.LineAmounts amounts =
                    InvoiceMath.line(draft.quantity(), draft.unitPrice(), draft.taxRate(), minorUnits);
            if (i < lines.size()) {
                lines.get(i).update(draft, amounts);
            } else {
                lines.add(new InvoiceLineItem(this, (short) (i + 1), draft, amounts));
            }
        }
        while (lines.size() > drafts.size()) {
            lines.remove(lines.size() - 1);
        }
        recalculateTotals();
    }

    void ensureEditable() {
        if (status != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVOICE_NOT_EDITABLE",
                    "Invoice " + describe() + " is " + status + "; only drafts can be changed.");
        }
    }

    void ensureDeletable() {
        if (status != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVOICE_NOT_DELETABLE",
                    "Invoice " + describe() + " is " + status + "; only drafts can be deleted. Void it instead.");
        }
    }

    // ----- Status changes -------------------------------------------------------------------

    /**
     * DRAFT → SENT. Checks run before the number is taken, so a refused send never asks the
     * counter for a number.
     */
    void send(Supplier<String> nextInvoiceNumber, Instant now) {
        if (status != InvoiceStatus.DRAFT) {
            throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Invoice " + describe() + " is already " + status + ".");
        }
        if (lines.isEmpty()) {
            throw new BusinessRuleException("INVOICE_HAS_NO_LINES", "Add at least one line item before sending.");
        }
        if (total.signum() <= 0) {
            throw new BusinessRuleException("INVOICE_TOTAL_ZERO", "An invoice with a total of zero cannot be sent.");
        }
        this.invoiceNumber = nextInvoiceNumber.get();
        this.status = InvoiceStatus.SENT;
        this.sentAt = now;
    }

    /** SENT → VOID, only while nothing has been paid. Drafts are deleted instead. */
    void voidInvoice(String reason, Instant now) {
        switch (status) {
            case DRAFT -> throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Drafts are deleted, not voided.");
            case VOID -> throw new ConflictException("INVALID_STATUS_TRANSITION",
                    "Invoice " + describe() + " is already void.");
            case PARTIALLY_PAID, PAID -> throw new ConflictException("INVOICE_HAS_PAYMENTS",
                    "Invoice " + describe() + " has payments recorded and cannot be voided.");
            case SENT -> {
                if (amountPaid.signum() > 0) {
                    throw new ConflictException("INVOICE_HAS_PAYMENTS",
                            "Invoice " + describe() + " has payments recorded and cannot be voided.");
                }
                this.status = InvoiceStatus.VOID;
                this.voidReason = reason;
                this.voidedAt = now;
            }
        }
    }

    // ----- Payments -------------------------------------------------------------------------

    /** Only issued invoices with money still owed take payments. */
    public void ensureAcceptsPayment() {
        switch (status) {
            case DRAFT -> throw new ConflictException("INVOICE_NOT_SENT",
                    "Invoice #" + id + " is a draft; send it before recording payments.");
            case PAID -> throw new ConflictException("INVOICE_ALREADY_PAID",
                    "Invoice " + describe() + " is already paid in full.");
            case VOID -> throw new ConflictException("INVOICE_VOID",
                    "Invoice " + describe() + " is void and cannot take payments.");
            case SENT, PARTIALLY_PAID -> {
                // accepted
            }
        }
    }

    /**
     * Adds a payment to the amount paid and moves the invoice to PARTIALLY_PAID, or to PAID when
     * nothing is left. Refuses amounts with too many decimals and anything above the balance due.
     */
    public void recordPayment(BigDecimal amount, Instant now) {
        ensureAcceptsPayment();
        int minorUnits = currency.getMinorUnits();
        if (InvoiceMath.exceedsPrecision(amount, minorUnits)) {
            throw new BadRequestException("INVALID_AMOUNT_PRECISION",
                    "amount has more decimal places than %s allows (%d).".formatted(currency.getCode(), minorUnits));
        }
        BigDecimal balance = getBalanceDue();
        if (amount.compareTo(balance) > 0) {
            throw new BusinessRuleException("OVERPAYMENT",
                    "A payment of %s %s is more than the balance due of %s %s.".formatted(
                            InvoiceMath.round(amount, minorUnits).toPlainString(), currency.getCode(),
                            InvoiceMath.round(balance, minorUnits).toPlainString(), currency.getCode()));
        }
        this.amountPaid = amountPaid.add(amount);
        if (getBalanceDue().signum() == 0) {
            this.status = InvoiceStatus.PAID;
            this.paidAt = now;
        } else {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
        this.updatedAt = now;
    }

    // ----- Derived values -------------------------------------------------------------------

    public boolean isOverdue(LocalDate today) {
        return status.isOutstanding() && dueDate.isBefore(today);
    }

    public BigDecimal getBalanceDue() {
        return total.subtract(amountPaid);
    }

    private void setHeader(Client client, Currency currency, LocalDate issueDate, LocalDate dueDate, String notes) {
        this.client = client;
        this.currency = currency;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.notes = notes;
    }

    private void recalculateTotals() {
        BigDecimal newSubtotal = BigDecimal.ZERO;
        BigDecimal newTax = BigDecimal.ZERO;
        for (InvoiceLineItem line : lines) {
            newSubtotal = newSubtotal.add(line.getLineSubtotal());
            newTax = newTax.add(line.getLineTax());
        }
        this.subtotal = newSubtotal;
        this.taxTotal = newTax;
        this.total = newSubtotal.add(newTax);
    }

    private String describe() {
        return invoiceNumber != null ? invoiceNumber : "#" + id;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ----- Getters --------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Client getClient() {
        return client;
    }

    public Currency getCurrency() {
        return currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxTotal() {
        return taxTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<InvoiceLineItem> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
