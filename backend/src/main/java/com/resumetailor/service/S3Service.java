package com.resumetailor.service;

import com.resumetailor.config.EnvironmentConfig;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

public class S3Service {

    private final S3Client s3Client;
    private final EnvironmentConfig config;

    public S3Service() {
        this.config = EnvironmentConfig.getInstance();
        Region region = Region.of(config.getAwsRegion());
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public String uploadResume(String base64Pdf) {
        byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
        String key = "resumes/" + UUID.randomUUID() + ".pdf";
        putRawPdf(key, pdfBytes);
        return key;
    }

    public void putRawPdf(String key, byte[] pdfBytes) {
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(config.getS3BucketName())
                .key(key)
                .contentType("application/pdf")
                .build();
        s3Client.putObject(putOb, RequestBody.fromBytes(pdfBytes));
    }

    public String getObjectAsBase64(String key) {
        ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(config.getS3BucketName()).key(key).build()
        );
        return Base64.getEncoder().encodeToString(object.asByteArray());
    }

    public void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(config.getS3BucketName()).key(key).build());
    }
}
