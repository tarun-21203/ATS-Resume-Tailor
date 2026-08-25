package com.resumetailor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsService {
    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public AnalyticsService() {
        this.dynamoDbClient = DynamoDbClient.builder().build();
        this.tableName = getTableName();
        logger.info("AnalyticsService initialized with table: {}", tableName);
    }

    private String getTableName() {
        String tableName = System.getenv("DYNAMODB_TABLE_NAME");
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalStateException("Required environment variable 'DYNAMODB_TABLE_NAME' is not set");
        }
        return tableName.trim();
    }

    public void trackEvent(String metricType) {
        try {
            long timestamp = System.currentTimeMillis();
            
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("metric_type", AttributeValue.builder().s(metricType).build());
            item.put("timestamp", AttributeValue.builder().n(String.valueOf(timestamp)).build());
            item.put("count", AttributeValue.builder().n("1").build());
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
            
            dynamoDbClient.putItem(request);
            logger.info("Tracked event: {} at {}", metricType, timestamp);
        } catch (Exception e) {
            logger.error("Failed to track event: {}", metricType, e);
            throw new RuntimeException("Failed to track analytics event", e);
        }
    }

    public Map<String, Long> getMetrics() {
        try {
            Map<String, Long> metrics = new HashMap<>();
            
            // Query for resume_tailored events
            long resumeCount = countMetricType("resume_tailored");
            metrics.put("resume_tailored", resumeCount);
            
            // Query for website_visit events
            long visitCount = countMetricType("website_visit");
            metrics.put("website_visit", visitCount);
            
            logger.info("Retrieved metrics: {}", metrics);
            return metrics;
        } catch (Exception e) {
            logger.error("Failed to retrieve metrics", e);
            throw new RuntimeException("Failed to retrieve analytics metrics", e);
        }
    }

    private long countMetricType(String metricType) {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":metricType", AttributeValue.builder().s(metricType).build());
            
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("metric_type = :metricType")
                    .expressionAttributeValues(expressionAttributeValues)
                    .select(Select.COUNT)
                    .build();
            
            QueryResponse response = dynamoDbClient.query(queryRequest);
            return response.count();
        } catch (Exception e) {
            logger.error("Failed to count metric type: {}", metricType, e);
            return 0;
        }
    }
}
