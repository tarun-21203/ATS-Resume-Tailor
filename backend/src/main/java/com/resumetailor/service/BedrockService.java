package com.resumetailor.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.resumetailor.config.EnvironmentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.ArrayList;
import java.util.List;

public class BedrockService {
    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    private static final Gson gson = new Gson();

    private final EnvironmentConfig config;
    private final BedrockRuntimeClient client;

    public BedrockService() {
        this.config = EnvironmentConfig.getInstance();
        this.client = BedrockRuntimeClient.builder().region(Region.of(config.getBedrockRegion())).build();
    }

    public String tailorResume(String resumeText, String jobDescription) {
        String prompt = "Tailor the resume text to the job description. Return valid JSON for resume content only.\n\nResume:\n"
                + resumeText + "\n\nJob Description:\n" + jobDescription;
        return invokeText(prompt);
    }

    public String tailorCoverLetter(String resumeText, String jobDescription) {
        String prompt = "Write a concise tailored cover letter for this resume and job description. Return plain text only.\n\nResume:\n"
                + resumeText + "\n\nJob Description:\n" + jobDescription;
        return invokeText(prompt);
    }

    public List<String> extractRequirements(String jobDescription) {
        String prompt = "Extract top 8 job requirements as a JSON array of strings from this JD:\n" + jobDescription;
        String raw = invokeText(prompt);
        try {
            JsonArray array = JsonParser.parseString(raw).getAsJsonArray();
            List<String> out = new ArrayList<>();
            array.forEach(item -> out.add(item.getAsString()));
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

    public double scoreAtsFit(String resumeText, String jobDescription) {
        String prompt = "Score ATS fit from 0-100 as number only for this resume and JD.\nResume:\n"
                + resumeText + "\n\nJD:\n" + jobDescription;
        String raw = invokeText(prompt).replaceAll("[^0-9.]", "");
        try {
            return Math.max(0, Math.min(100, Double.parseDouble(raw)));
        } catch (Exception e) {
            return 0;
        }
    }

    private String invokeText(String prompt) {
        JsonObject request = new JsonObject();
        request.addProperty("anthropic_version", "bedrock-2023-05-31");
        request.addProperty("max_tokens", 1600);
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        JsonArray content = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("type", "text");
        text.addProperty("text", prompt);
        content.add(text);
        message.add("content", content);
        messages.add(message);
        request.add("messages", messages);

        InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                .modelId(config.getBedrockModelId())
                .contentType("application/json")
                .accept("application/json")
                .body(SdkBytes.fromUtf8String(gson.toJson(request)))
                .build();

        InvokeModelResponse response = client.invokeModel(invokeRequest);
        String body = response.body().asUtf8String();
        logger.debug("Bedrock response size: {}", body.length());

        try {
            JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
            JsonArray responseContent = parsed.getAsJsonArray("content");
            if (responseContent != null && !responseContent.isEmpty()) {
                return responseContent.get(0).getAsJsonObject().get("text").getAsString().trim();
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
