package com.resumetailor.model;

public record EmailRequest(String email, String pdfBase64, String coverLetterPdfBase64, String jobUrl) {
}
