package com.resumetailor.model;

import java.util.List;

public record ResumeResponse(
        String pdfBase64,
        String status,
        String coverLetterPdfBase64,
        Double atsScore,
        List<String> extractedRequirements
) {
}
