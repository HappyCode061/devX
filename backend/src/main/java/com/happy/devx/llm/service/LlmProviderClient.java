package com.happy.devx.llm.service;

import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;

public interface LlmProviderClient {

    /**
     * Indicates whether this client can serve the configured provider name.
     */
    boolean supports(String provider);

    /**
     * Generates an answer using the provider-specific implementation.
     */
    LlmGenerationResponse generateAnswer(LlmGenerationRequest request);
}
