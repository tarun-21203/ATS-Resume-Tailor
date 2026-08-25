package com.resumetailor.config;

public class EnvironmentConfig {

    private static EnvironmentConfig instance;

    private final String bedrockModelId;
    private final String bedrockRegion;
    private final String sesSourceEmail;
    private final String s3BucketName;
    private final String awsRegion;
    private final String dynamoDbTableName;
    private final String masterResumeTableName;

    private EnvironmentConfig() {
        this.bedrockModelId = getEnvOrDefault("BEDROCK_MODEL_ID", "anthropic.claude-3-haiku-20240307-v1:0");
        this.bedrockRegion = getEnvOrDefault("BEDROCK_REGION", getEnvOrDefault("AWS_REGION", "us-east-1"));
        this.sesSourceEmail = getEnvOrThrow("SES_SOURCE_EMAIL");
        this.s3BucketName = getEnvOrThrow("S3_BUCKET_NAME");
        this.awsRegion = getEnvOrDefault("AWS_REGION", "us-east-1");
        this.dynamoDbTableName = getEnvOrDefault("DYNAMODB_TABLE_NAME", "resume-tailor-analytics");
        this.masterResumeTableName = getEnvOrDefault("MASTER_RESUME_TABLE_NAME", "resume-tailor-master-resume");
    }

    public static synchronized EnvironmentConfig getInstance() {
        if (instance == null) {
            instance = new EnvironmentConfig();
        }
        return instance;
    }

    private String getEnvOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(String.format("Required environment variable '%s' is not set", key));
        }
        return value.trim();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
    }

    public String getBedrockModelId() { return bedrockModelId; }
    public String getBedrockRegion() { return bedrockRegion; }
    public String getSesSourceEmail() { return sesSourceEmail; }
    public String getS3BucketName() { return s3BucketName; }
    public String getAwsRegion() { return awsRegion; }
    public String getDynamoDbTableName() { return dynamoDbTableName; }
    public String getMasterResumeTableName() { return masterResumeTableName; }
}
