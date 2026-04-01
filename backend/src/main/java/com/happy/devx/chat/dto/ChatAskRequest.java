package com.happy.devx.chat.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ChatAskRequest(
        @NotBlank(message = "question is required")
        String question,
        @Min(value = 1, message = "retrievalLimit must be at least 1")
        @Max(value = 10, message = "retrievalLimit must be at most 10")
        int retrievalLimit,
        boolean includeDebug
) {
}
