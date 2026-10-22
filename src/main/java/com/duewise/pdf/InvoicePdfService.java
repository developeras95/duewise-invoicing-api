package com.duewise.pdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders an invoice as a PDF in two steps: Thymeleaf fills the XHTML template
 * (templates/pdf/invoice.html), then OpenHTMLtoPDF lays it out as an A4 page. Changing the look of
 * the invoice means editing HTML and CSS, not Java.
 */
@Service
public class InvoicePdfService {

    private final ITemplateEngine templates;

    public InvoicePdfService(ITemplateEngine templates) {
        this.templates = templates;
    }

    public byte[] render(InvoiceDocument document) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("doc", document);
        String xhtml = templates.process("pdf/invoice", context);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not render the PDF for invoice " + document.label(), e);
        }
    }
}
