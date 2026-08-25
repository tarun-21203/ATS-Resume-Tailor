package com.resumetailor.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfServiceTest {

    @Test
    void generatePdfFromPlainTextReturnsBase64() throws Exception {
        PdfService service = new PdfService();
        String pdf = service.generatePdf("Hello world");
        assertNotNull(pdf);
        assertFalse(pdf.isBlank());
    }
}
