package com.happy.devx.llm.service;

import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingLlmGenerationService implements LlmGenerationService {

    private final LlmProperties llmProperties;
    private final List<LlmProviderClient> providerClients;

    /**
     * Routes answer generation to the configured provider, with fallback if unavailable or disabled.
     */
    @Override
    public LlmGenerationResponse generateAnswer(LlmGenerationRequest request) {
        String configuredProvider = llmProperties.provider();
        LlmProviderClient fallbackClient = resolveProvider("draft");

        if (!llmProperties.enabled()) {
            log.info("LLM generation is disabled; using fallback provider");
            return fallbackClient.generateAnswer(request);
        }

        try {
            LlmProviderClient providerClient = resolveProvider(configuredProvider);
            return providerClient.generateAnswer(request);
        } catch (Exception exception) {
            log.warn("Falling back from provider={} due to error: {}", configuredProvider, exception.getMessage());
            return fallbackClient.generateAnswer(request);
        }
    }

    private LlmProviderClient resolveProvider(String provider) {
        return providerClients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No LLM provider client found for provider=" + provider));
    }
}
