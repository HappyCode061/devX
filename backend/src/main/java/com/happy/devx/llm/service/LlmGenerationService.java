package com.happy.devx.llm.service;

import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;

public interface LlmGenerationService {

    /**
     * Generates an answer for the provided grounded retrieval context.
     */
    LlmGenerationResponse generateAnswer(LlmGenerationRequest request);
}
