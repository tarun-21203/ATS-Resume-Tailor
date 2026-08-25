package com.resumetailor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.resumetailor.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class GetAnalyticsHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final Logger logger = LoggerFactory.getLogger(GetAnalyticsHandler.class);
    private final Gson gson = new Gson();
    private final AnalyticsService analyticsService = new AnalyticsService();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            logger.info("Processing get analytics request");

            Map<String, Long> metrics = analyticsService.getMetrics();

            return Map.of(
                "statusCode", 200,
                "headers", Map.of(
                    "Content-Type", "application/json",
                    "Access-Control-Allow-Origin", "*"
                ),
                "body", gson.toJson(metrics)
            );

        } catch (Exception e) {
            logger.error("Error retrieving analytics", e);
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
