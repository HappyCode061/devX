package com.happy.devx.llm.dto;

public record LlmGenerationResponse(
        String answer,
        String provider,
        String model,
        boolean fallbackUsed
) {
}
