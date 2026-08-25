package com.resumetailor.service;

import com.resumetailor.config.EnvironmentConfig;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

public class MasterResumeMetadataService {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
    private final String tableName = EnvironmentConfig.getInstance().getMasterResumeTableName();

    public void put(String userId, String s3Key) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("user_id", AttributeValue.builder().s(userId).build());
        item.put("s3_key", AttributeValue.builder().s(s3Key).build());
        item.put("updated_at", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    public String getS3Key(String userId) {
        Map<String, AttributeValue> key = Map.of("user_id", AttributeValue.builder().s(userId).build());
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder().tableName(tableName).key(key).build()).item();
        if (item == null || !item.containsKey("s3_key")) {
            return null;
        }
        return item.get("s3_key").s();
    }

    public void delete(String userId) {
        Map<String, AttributeValue> key = Map.of("user_id", AttributeValue.builder().s(userId).build());
        dynamoDbClient.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(key).build());
    }
}
