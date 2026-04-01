package com.happy.devx.chat.service;

import com.happy.devx.chat.dto.ChatAskResponse;
import com.happy.devx.chat.dto.ChatCitationResponse;
import com.happy.devx.chat.dto.ChatDebugResponse;
import com.happy.devx.chat.dto.ChatDebugSourceResponse;
import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.llm.dto.LlmGenerationRequest;
import com.happy.devx.llm.dto.LlmGenerationResponse;
import com.happy.devx.llm.service.LlmGenerationService;
import com.happy.devx.llm.service.LlmPromptBuilder;
import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.dto.RetrievedDocumentResponse;
import com.happy.devx.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CITATIONS = 8;
    private static final int MAX_DOCUMENTS_FOR_CITATIONS = 4;
    private static final int MAX_CHUNKS_PER_DOCUMENT_FOR_CITATIONS = 2;
    private static final int MAX_PROMPT_PREVIEW_CHARS = 1200;

    private final RetrievalService retrievalService;
    private final LlmGenerationService llmGenerationService;
    private final LlmPromptBuilder llmPromptBuilder;
    private final LlmProperties llmProperties;

    /**
     * Builds a grounded chat response from retrieval output through the configured generation service.
     */
    @Transactional(readOnly = true)
    public ChatAskResponse ask(String question, int retrievalLimit, boolean includeDebug) {
        log.info("Chat ask question='{}' retrievalLimit={} includeDebug={}", question, retrievalLimit, includeDebug);

        RetrievalSearchResponse retrieval = retrievalService.search(question, retrievalLimit);
        LlmGenerationResponse generation = llmGenerationService.generateAnswer(new LlmGenerationRequest(question, retrieval));
        List<ChatCitationResponse> citations = buildCitations(retrieval.documents());
        ChatDebugResponse debug = includeDebug ? buildDebugResponse(retrieval, generation.fallbackUsed(), question) : null;

        return new ChatAskResponse(
                question,
                generation.answer(),
                generation.provider(),
                generation.model(),
                generation.fallbackUsed(),
                retrieval.matchedTerms(),
                retrieval.matchedDocumentCount(),
                retrieval.matchedChunkCount(),
                citations,
                debug
        );
    }

    /**
     * Flattens retrieval matches into citation records while keeping the list bounded.
     */
    private List<ChatCitationResponse> buildCitations(List<RetrievedDocumentResponse> documents) {
        List<ChatCitationResponse> citations = new ArrayList<>();

        documents.stream()
                .limit(MAX_DOCUMENTS_FOR_CITATIONS)
                .forEach(document -> document.matchedChunks().stream()
                        .limit(MAX_CHUNKS_PER_DOCUMENT_FOR_CITATIONS)
                        .forEach(chunk -> {
                if (citations.size() >= MAX_CITATIONS) {
                    return;
                }

                citations.add(
                    new ChatCitationResponse(
                            document.documentId(),
                            document.documentTitle(),
                            document.documentSourcePath(),
                            chunk.chunkId(),
                            chunk.chunkIndex()
                    )
                );
                        }));

        return citations;
    }

    /**
     * Provides optional observability details for A/B testing fallback and provider-backed chat responses.
     */
    private ChatDebugResponse buildDebugResponse(
            RetrievalSearchResponse retrieval,
            boolean fallbackUsed,
            String question
    ) {
        List<RetrievedDocumentResponse> selectedDocuments = llmPromptBuilder.selectContextDocuments(retrieval, llmProperties);
        String promptPreview = llmPromptBuilder.buildUserPrompt(question, retrieval, llmProperties);
        if (promptPreview.length() > MAX_PROMPT_PREVIEW_CHARS) {
            promptPreview = promptPreview.substring(0, MAX_PROMPT_PREVIEW_CHARS) + "...";
        }

        List<ChatDebugSourceResponse> selectedSources = selectedDocuments.stream()
                .map(document -> new ChatDebugSourceResponse(
                        document.documentId(),
                        document.documentTitle(),
                        document.documentSourcePath(),
                        document.matchedTerms(),
                        document.matchedChunks().stream()
                                .map(chunk -> chunk.chunkIndex())
                                .toList()
                ))
                .toList();

        return new ChatDebugResponse(
                fallbackUsed ? "fallback" : "provider",
                promptPreview,
                promptPreview.length(),
                selectedSources
        );
    }
}
