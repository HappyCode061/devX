package com.happy.devx.llm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OpenAiResponsesProviderClient implements LlmProviderClient {

    private final LlmProperties llmProperties;
    private final LlmPromptBuilder llmPromptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Supports the OpenAI provider using the Responses API.
     */
    @Override
    public boolean supports(String provider) {
        return "openai".equalsIgnoreCase(provider);
    }

    /**
     * Calls the OpenAI Responses API and returns the generated text answer.
     */
    @Override
    public LlmGenerationResponse generateAnswer(LlmGenerationRequest request) {
        if (llmProperties.apiKey() == null || llmProperties.apiKey().isBlank()) {
            throw new IllegalStateException("LLM API key is required for the OpenAI provider");
        }

        String baseUrl = normalizeBaseUrl(
                (llmProperties.baseUrl() == null || llmProperties.baseUrl().isBlank())
                        ? "https://api.openai.com"
                        : llmProperties.baseUrl()
        );
        String responsesPath = resolveResponsesPath(baseUrl);

        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + llmProperties.apiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", llmProperties.model());
        payload.put("instructions", llmPromptBuilder.buildInstructions());
        payload.put("input", llmPromptBuilder.buildUserPrompt(request.question(), request.retrieval(), llmProperties));

        log.info("Calling OpenAI Responses API baseUrl={} path={}", baseUrl, responsesPath);
        JsonNode response = client.post()
                .uri(responsesPath)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String answer = extractOutputText(response);
        log.info("Generated OpenAI response using model={}", llmProperties.model());

        return new LlmGenerationResponse(
                answer,
                "openai",
                llmProperties.model(),
                false
        );
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response body was empty");
        }

        JsonNode output = response.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        if ("output_text".equals(contentItem.path("type").asText())) {
                            String text = contentItem.path("text").asText();
                            if (!text.isBlank()) {
                                return text;
                            }
                        }
                    }
                }
            }
        }

        JsonNode outputText = response.path("output_text");
        if (outputText.isArray()) {
            List<String> texts = objectMapper.convertValue(outputText, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            String joined = String.join("\n", texts).trim();
            if (!joined.isBlank()) {
                return joined;
            }
        }

        throw new IllegalStateException("OpenAI response did not contain output text");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveResponsesPath(String baseUrl) {
        URI uri = URI.create(baseUrl);
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (path.endsWith("/v1")) {
            return "/responses";
        }

        if (path.isBlank() || "/".equals(path)) {
            return "/v1/responses";
        }

        return path + "/responses";
    }
}
