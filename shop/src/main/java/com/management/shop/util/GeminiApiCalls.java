package com.management.shop.util;

import ch.qos.logback.core.CoreConstants;
import com.management.shop.entity.GeminiTextExtract;
import com.management.shop.repository.ApiSaveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiApiCalls {

    @Value("${gemini.flash.apikey}")
    private String geminiApiKey;

    @Value("${gemini.flash.url}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate;

    public GeminiApiCalls() {
        this.restTemplate = new RestTemplate();
    }

    @Autowired
    ApiSaveRepository apiLogSaveRepo;

    public String extractUsername() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // For testing purposes, you might uncomment the line below
        // username="junaid1";
        return username;
    }


    public String geminiApiCall(String base64Image, String mimeType) {

        String promptText = "Analyze this image and extract all products. " +
                "Return ONLY a valid CSV format with the following exact headers on the first line: " +
                "name,hsn,category,costPrice,price,stock,tax,location. " +
                "Rules: " +

                "1. category should be 'Product' if not specified. " +
                "2. costPrice should be same as price if not specified. " +
                "3. tax should be 0 if not specified. " +
                "4. stock should be 1 if not specified. " +
                "5. location should be blank if not specified. " +
                "Do not include markdown formatting like ```csv or any other text.";

        // 4. Build the JSON payload request body
        Map<String, Object> inlineData = new HashMap<>();
        inlineData.put("mimeType", mimeType);
        inlineData.put("data", base64Image);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("inlineData", inlineData);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", promptText);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", List.of(textPart, imagePart));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url=geminiApiUrl+geminiApiKey;
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        System.out.println("Gemini API response: " + response.getBody());

        var apiLog = GeminiTextExtract.builder().createdDate(LocalDateTime.now())
                .username(extractUsername())
                .request("geminiPrompt")
                .name("Gemini Text Extraction API")
                .url(geminiApiUrl)
                .status(response.getStatusCode().toString())
                .response("response size: "+response.getBody().toString().length())
                .build();

        try {
            apiLogSaveRepo.save(apiLog);
        } catch (Exception e) {
            log.info("Error while saving logs for Gemini API call: " + e.getMessage());
        }


        return extractGeminiResponse(response.getBody());
    }

    private String extractGeminiResponse(Map responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    String extractedText = (String) parts.get(0).get("text");
                    // Clean up any potential markdown formatting the model might still try to add
                    return extractedText.replace("```csv\n", "").replace("```", "").trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
        }
        throw new RuntimeException("Failed to extract text from Gemini response");
    }


}
