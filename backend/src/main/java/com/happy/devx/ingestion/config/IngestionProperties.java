package com.happy.devx.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devx.ingestion")
public record IngestionProperties(
        boolean enabled,
        String sampleKnowledgeBasePath
) {
}
