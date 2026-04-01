package com.happy.devx.llm.service;

import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FallbackLlmProviderClient implements LlmProviderClient {

    private final DraftAnswerComposer draftAnswerComposer;
    private final LlmProperties llmProperties;

    /**
     * Supports the local fallback and draft provider modes.
     */
    @Override
    public boolean supports(String provider) {
        return provider == null || provider.isBlank() || "draft".equalsIgnoreCase(provider) || "fallback".equalsIgnoreCase(provider);
    }

    /**
     * Returns the deterministic draft answer until a real provider-backed implementation is selected.
     */
    @Override
    public LlmGenerationResponse generateAnswer(LlmGenerationRequest request) {
        log.info(
                "Using fallback LLM generation client provider={} model={} enabled={}",
                llmProperties.provider(),
                llmProperties.model(),
                llmProperties.enabled()
        );

        return new LlmGenerationResponse(
                draftAnswerComposer.composeDraftAnswer(request.question(), request.retrieval()),
                "draft",
                "retrieval-draft-v1",
                true
        );
    }
}
