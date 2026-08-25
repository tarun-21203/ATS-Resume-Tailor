package com.resumetailor.model;

public record ResumeRequest(String resumePdfBase64, String jobDescription, Boolean includeCoverLetter) {
}
