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
        log.info("Entered geminiApiCall with mimeType={}, imagePayloadSize={}", mimeType,
                base64Image != null ? base64Image.length() : 0);

    /*    String promptText = "Analyze this image and extract all products. " +
                "Return ONLY a valid CSV format with the following exact headers on the first line: " +
                "name,hsn,category,costPrice,price,stock,tax,location. " +
                "Rules: " +

                "1. category should be 'Product' if not specified. " +
                "2. costPrice should be same as price if not specified. " +
                "3. tax should be 0 if not specified. " +
                "4. stock should be 1 if not specified. " +
                "5. location should be blank if not specified. " +
                "Do not include markdown formatting like ```csv or any other text.";*/

        String promptText ="Extract all products from this image as raw CSV.\n" +
                "Headers: name,hsn,category,costPrice,price,stock,tax,location\n" +
                "\n" +
                "Rules for missing data:\n" +
                "\n" +
                "category: 'Product'\n" +
                "\n" +
                "costPrice: use price\n" +
                "\n" +
                "tax: 0\n" +
                "\n" +
                "stock: 1\n" +
                "\n" +
                "location: leave blank\n" +
                "\n" +
                "Output ONLY raw CSV text. No markdown blocks, no formatting, no explanations.";

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
        log.info("Sending Gemini request to configured URL, partCount={}", ((List<?>) content.get("parts")).size());
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        log.info("Received Gemini response with status={} and bodyPresent={}",
                response.getStatusCode(), response.getBody() != null);

        var apiLog = GeminiTextExtract.builder().createdDate(LocalDateTime.now())
                .username(extractUsername())
                .request("geminiPrompt")
                .name("Gemini Text Extraction API")
                .url(geminiApiUrl)
                .status(response.getStatusCode().toString())
                .response("response size: " + (response.getBody() != null ? response.getBody().toString().length() : 0))
                .build();

        try {
            apiLogSaveRepo.save(apiLog);
            log.info("Saved Gemini API call log entry for user={}", extractUsername());
        } catch (Exception e) {
            log.error("Error while saving logs for Gemini API call", e);
        }

        String extracted = extractGeminiResponse(response.getBody());
        log.info("Gemini response parsed successfully, extractedTextLength={}",
                extracted != null ? extracted.length() : 0);
        return extracted;
    }

    private String extractGeminiResponse(Map responseBody) {
        try {
            log.info("Parsing Gemini response body");
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    String extractedText = (String) parts.get(0).get("text");
                    // Clean up any potential markdown formatting the model might still try to add
                    log.info("Gemini response contained extracted text, candidateCount={}, partCount={}",
                            candidates.size(), parts.size());
                    return extractedText.replace("```csv\n", "").replace("```", "").trim();
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Gemini response", e);
        }
        log.warn("Failed to extract text from Gemini response body");
        throw new RuntimeException("Failed to extract text from Gemini response");
    }


}
