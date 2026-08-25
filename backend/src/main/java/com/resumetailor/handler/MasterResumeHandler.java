package com.resumetailor.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.resumetailor.service.MasterResumeMetadataService;
import com.resumetailor.service.S3Service;

import java.util.Base64;
import java.util.Map;

public class MasterResumeHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final Gson gson = new Gson();
    private final MasterResumeMetadataService metadataService = new MasterResumeMetadataService();
    private final S3Service s3Service = new S3Service();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            String method = (String) input.get("requestContext") != null
                    ? (String) ((Map<String, Object>) ((Map<String, Object>) input.get("requestContext")).get("http")).get("method")
                    : (String) input.getOrDefault("httpMethod", "GET");

            Map<String, String> headers = (Map<String, String>) input.get("headers");
            String userId = headers != null ? headers.getOrDefault("x-user-id", "anonymous") : "anonymous";
            String key = "users/" + userId + "/master.pdf";

            if ("PUT".equalsIgnoreCase(method)) {
                String body = (String) input.get("body");
                JsonObject request = gson.fromJson(body, JsonObject.class);
                String resumePdfBase64 = request.get("resumePdfBase64").getAsString();
                byte[] bytes = Base64.getDecoder().decode(resumePdfBase64);
                s3Service.putRawPdf(key, bytes);
                metadataService.put(userId, key);
                return ok(Map.of("message", "Master resume saved", "s3Key", key));
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                String storedKey = metadataService.getS3Key(userId);
                if (storedKey != null) {
                    s3Service.deleteObject(storedKey);
                    metadataService.delete(userId);
                }
                return ok(Map.of("message", "Master resume deleted"));
            }

            String storedKey = metadataService.getS3Key(userId);
            if (storedKey == null) {
                return Map.of("statusCode", 404, "headers", cors(), "body", gson.toJson(Map.of("error", "No master resume found")));
            }

            String base64 = s3Service.getObjectAsBase64(storedKey);
            return ok(Map.of("s3Key", storedKey, "resumePdfBase64", base64));
        } catch (Exception e) {
            return Map.of("statusCode", 500, "headers", cors(), "body", gson.toJson(Map.of("error", e.getMessage())));
        }
    }

    private Map<String, Object> ok(Object body) {
        return Map.of("statusCode", 200, "headers", cors(), "body", gson.toJson(body));
    }

    private Map<String, String> cors() {
        return Map.of("Content-Type", "application/json", "Access-Control-Allow-Origin", "*");
    }
}
