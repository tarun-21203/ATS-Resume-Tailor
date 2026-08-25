package com.resumetailor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.resumetailor.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AnalyticsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsHandler.class);
    private final Gson gson = new Gson();
    private final AnalyticsService analyticsService = new AnalyticsService();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            logger.info("Processing analytics tracking request");

            String body = (String) input.get("body");
            @SuppressWarnings("unchecked")
            Map<String, String> requestBody = gson.fromJson(body, Map.class);
            
            String metricType = requestBody.get("metricType");
            
            if (metricType == null || metricType.isEmpty()) {
                logger.error("Missing metricType in request");
                return Map.of(
                    "statusCode", 400,
                    "headers", Map.of(
                        "Content-Type", "application/json",
                        "Access-Control-Allow-Origin", "*"
                    ),
                    "body", gson.toJson(Map.of("error", "metricType is required"))
                );
            }

            analyticsService.trackEvent(metricType);

            return Map.of(
                "statusCode", 200,
                "headers", Map.of(
                    "Content-Type", "application/json",
                    "Access-Control-Allow-Origin", "*"
                ),
                "body", gson.toJson(Map.of("status", "success"))
            );

        } catch (Exception e) {
            logger.error("Error tracking analytics", e);
            return Map.of(
                "statusCode", 500,
                "headers", Map.of(
                    "Content-Type", "application/json",
                    "Access-Control-Allow-Origin", "*"
                ),
                "body", gson.toJson(Map.of("error", e.getMessage()))
            );
        }
    }
}
