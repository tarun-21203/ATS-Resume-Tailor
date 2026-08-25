package com.resumetailor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.resumetailor.model.ResumeRequest;
import com.resumetailor.model.ResumeResponse;
import com.resumetailor.service.AnalyticsService;
import com.resumetailor.service.BedrockService;
import com.resumetailor.service.PdfService;
import com.resumetailor.service.S3Service;

import java.util.Map;

public class TailorResumeHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final Gson gson = new Gson();
    private final BedrockService bedrockService = new BedrockService();
    private final PdfService pdfService = new PdfService();
    private final S3Service s3Service = new S3Service();
    private final AnalyticsService analyticsService = new AnalyticsService();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            String body = (String) input.get("body");
            ResumeRequest request = gson.fromJson(body, ResumeRequest.class);

            if (request == null || request.resumePdfBase64() == null || request.resumePdfBase64().isBlank() ||
                    request.jobDescription() == null || request.jobDescription().isBlank()) {
                return Map.of(
                        "statusCode", 400,
                        "headers", Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"),
                        "body", gson.toJson(Map.of("error", "resumePdfBase64 and jobDescription are required"))
                );
            }

            String storageKey = s3Service.uploadResume(request.resumePdfBase64());
            String resumeText = pdfService.extractTextFromPdf(request.resumePdfBase64());
            String tailoredText = bedrockService.tailorResume(resumeText, request.jobDescription());
            String pdfBase64 = pdfService.generatePdf(tailoredText);

            String coverLetterPdf = null;
            if (Boolean.TRUE.equals(request.includeCoverLetter())) {
                String coverLetterText = bedrockService.tailorCoverLetter(resumeText, request.jobDescription());
                coverLetterPdf = pdfService.generatePdf(coverLetterText);
            }

            double atsScore = bedrockService.scoreAtsFit(resumeText, request.jobDescription());

            try {
                analyticsService.trackEvent("resume_tailored");
            } catch (Exception ignored) {
            }

            ResumeResponse response = new ResumeResponse(
                    pdfBase64,
                    "Success",
                    coverLetterPdf,
                    atsScore,
                    bedrockService.extractRequirements(request.jobDescription())
            );

            return Map.of(
                    "statusCode", 200,
                    "headers", Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"),
                    "body", gson.toJson(Map.of("result", response, "storageKey", storageKey))
            );

        } catch (Exception e) {
            return Map.of(
                    "statusCode", 500,
                    "headers", Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*"),
                    "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
