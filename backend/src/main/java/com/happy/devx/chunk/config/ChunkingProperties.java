package com.happy.devx.chunk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devx.chunking")
public record ChunkingProperties(
        boolean enabled,
        int targetSize,
        int overlapSize
) {
}
