package com.happy.devx.llm.service;

import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;
import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackLlmGenerationServiceTest {

    @Test
    void usesConfiguredProviderMetadataWhileReturningDraftAnswer() {
        DraftAnswerComposer composer = new DraftAnswerComposer();
        LlmProperties properties = new LlmProperties(false, "draft", "retrieval-draft-v1", "", "", 2000, 3, 1);
        FallbackLlmProviderClient service = new FallbackLlmProviderClient(composer, properties);

        LlmGenerationResponse response = service.generateAnswer(
                new LlmGenerationRequest(
                        "how to setup postgres",
                        new RetrievalSearchResponse("how to setup postgres", List.of("postgresql"), 5, 0, 0, List.of())
                )
        );

        assertThat(response.provider()).isEqualTo("draft");
        assertThat(response.model()).isEqualTo("retrieval-draft-v1");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.answer()).contains("I could not find matching knowledge base context");
    }
}
