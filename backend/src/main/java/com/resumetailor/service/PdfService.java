package com.resumetailor.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    public String extractTextFromPdf(String base64Pdf) throws IOException {
        logger.debug("Extracting text from PDF");
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            logger.info("Successfully extracted text from PDF, length: {}", text.length());
            return text;
        }
    }

    public String generatePdf(String content) throws IOException {
        logger.info("Generating PDF from content, length: {}", content.length());

        String trimmed = content.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                logger.debug("Content appears to be JSON, using ResumePdfGenerator");
                ResumePdfGenerator generator = new ResumePdfGenerator();
                String result = generator.generatePdfFromJson(content);
                logger.info("PDF generated successfully from JSON");
                return result;
            } catch (Exception e) {
                logger.error("JSON PDF generation failed", e);
                throw new IOException("Failed to generate PDF from JSON: " + e.getMessage(), e);
            }
        } else {
            logger.debug("Content is plain text, using simple PDF generator");
            return generateSimplePdf(content);
        }
    }

    private String generateSimplePdf(String content) throws IOException {
        logger.debug("Generating simple text-based PDF");
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);

            org.apache.pdfbox.pdmodel.PDPageContentStream contentStream =
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page);
            contentStream.setFont(
                    new org.apache.pdfbox.pdmodel.font.PDType1Font(
                            org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA
                    ),
                    12
            );
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 700);
            contentStream.setLeading(14.5f);

            String[] lines = content.split("\n");
            for (String line : lines) {
                // Simple wrapping logic
                if (line.length() > 90) {
                    int start = 0;
                    while (start < line.length()) {
                        int end = Math.min(start + 90, line.length());
                        String sub = line.substring(start, end);
                        contentStream.showText(sub.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""));
                        contentStream.newLine();
                        start += 90;
                    }
                } else {
                    contentStream.showText(line.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""));
                    contentStream.newLine();
                }
            }
            contentStream.endText();
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            logger.info("Simple PDF generated successfully, size: {} bytes", baos.size());
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }
}
