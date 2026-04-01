package com.happy.devx.llm.dto;

import com.happy.devx.retrieval.dto.RetrievalSearchResponse;

public record LlmGenerationRequest(
        String question,
        RetrievalSearchResponse retrieval
) {
}
