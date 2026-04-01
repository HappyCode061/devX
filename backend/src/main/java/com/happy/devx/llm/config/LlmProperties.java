package com.happy.devx.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devx.llm")
public record LlmProperties(
        boolean enabled,
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        int maxContextCharacters,
        int maxContextDocuments,
        int maxContextChunksPerDocument
) {
}
