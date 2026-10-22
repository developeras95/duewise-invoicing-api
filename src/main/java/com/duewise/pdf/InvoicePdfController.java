package com.duewise.pdf;

import java.time.Clock;
import java.time.LocalDate;

import com.duewise.config.BusinessProperties;
import com.duewise.invoice.Invoice;
import com.duewise.invoice.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Invoices")
public class InvoicePdfController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService pdfService;
    private final BusinessProperties business;
    private final Clock clock;

    public InvoicePdfController(InvoiceService invoiceService, InvoicePdfService pdfService,
                                BusinessProperties business, Clock clock) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.business = business;
        this.clock = clock;
    }

    @GetMapping(value = "/api/v1/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download an invoice as PDF",
            description = "A4 invoice with business and client details, lines, totals and balance due. "
                    + "Drafts are marked DRAFT, paid invoices PAID, void invoices VOID.")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Invoice invoice = invoiceService.requireWithDetails(id);
        InvoiceDocument document = InvoiceDocument.from(invoice, business, LocalDate.now(clock));
        byte[] pdf = pdfService.render(document);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(document.fileName()).build().toString())
                .body(pdf);
    }
}
