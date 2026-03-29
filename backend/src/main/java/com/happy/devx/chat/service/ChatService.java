package com.happy.devx.chat.service;

import com.happy.devx.chat.dto.ChatAskResponse;
import com.happy.devx.chat.dto.ChatCitationResponse;
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

    private static final int MAX_DOCUMENTS_IN_ANSWER = 3;
    private static final int MAX_CHUNKS_PER_DOCUMENT_IN_ANSWER = 1;
    private static final int MAX_CHARS_PER_CHUNK_EXCERPT = 240;
    private static final int MAX_CITATIONS = 8;
    private static final String NL = "\n";

    private final RetrievalService retrievalService;

    /**
     * Builds a grounded draft chat response from retrieval output without using an LLM.
     */
    @Transactional(readOnly = true)
    public ChatAskResponse ask(String question, int retrievalLimit) {
        log.info("Chat ask question='{}' retrievalLimit={}", question, retrievalLimit);

        RetrievalSearchResponse retrieval = retrievalService.search(question, retrievalLimit);
        String answer = buildDraftAnswer(question, retrieval);
        List<ChatCitationResponse> citations = buildCitations(retrieval.documents());

        return new ChatAskResponse(
                question,
                answer,
                retrieval.matchedTerms(),
                retrieval.matchedDocumentCount(),
                retrieval.matchedChunkCount(),
                citations
        );
    }

    /**
     * Shapes a readable, bounded answer for Postman and early UI testing.
     */
    private String buildDraftAnswer(String question, RetrievalSearchResponse retrieval) {
        if (retrieval.documents().isEmpty()) {
            return "I could not find matching knowledge base context for that question yet.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Question: ").append(question).append(NL).append(NL);
        builder.append("Summary:").append(NL);
        builder.append("I found relevant knowledge base context in ").append(retrieval.matchedDocumentCount())
                .append(" document(s) across ").append(retrieval.matchedChunkCount()).append(" chunk(s).")
                .append(" The strongest matches are summarized below.").append(NL);

        if (!retrieval.matchedTerms().isEmpty()) {
            builder.append("Matched terms: ").append(String.join(", ", retrieval.matchedTerms())).append(NL);
        }

        builder.append(NL);

        builder.append("Key findings:").append(NL);

        retrieval.documents().stream()
                .limit(MAX_DOCUMENTS_IN_ANSWER)
                .forEach(document -> {
            builder.append("- ").append(document.documentTitle())
                    .append(": ");

            document.matchedChunks().stream()
                    .limit(MAX_CHUNKS_PER_DOCUMENT_IN_ANSWER)
                    .forEach(chunk -> builder.append(abbreviate(chunk.content())));

            if (!document.matchedTerms().isEmpty()) {
                builder.append(" (matched: ").append(String.join(", ", document.matchedTerms())).append(")");
            }

            builder.append(NL);
                });

        builder.append(NL).append("Sources used:").append(NL);
        retrieval.documents().stream()
                .limit(MAX_DOCUMENTS_IN_ANSWER)
                .forEach(document -> builder.append("- ")
                        .append(document.documentTitle())
                        .append(" (")
                        .append(document.documentSourcePath())
                        .append(")")
                        .append(NL));

        builder.append(NL).append("This is a retrieval-backed draft, not a model-generated final answer.");
        return builder.toString().trim();
    }

    /**
     * Flattens retrieval matches into citation records while keeping the list bounded.
     */
    private List<ChatCitationResponse> buildCitations(List<RetrievedDocumentResponse> documents) {
        List<ChatCitationResponse> citations = new ArrayList<>();

        for (RetrievedDocumentResponse document : documents) {
            document.matchedChunks().forEach(chunk -> {
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
            });
        }

        return citations;
    }

    /**
     * Normalizes raw chunk text into a short excerpt that reads cleanly in API responses.
     */
    private String abbreviate(String content) {
        String normalized = content
                .replaceAll("(?m)^#+\\s*", "")
                .replaceAll("(?m)^-\\s*", "")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= MAX_CHARS_PER_CHUNK_EXCERPT) {
            return normalized;
        }
        int cutIndex = normalized.lastIndexOf(' ', MAX_CHARS_PER_CHUNK_EXCERPT - 3);
        if (cutIndex < MAX_CHARS_PER_CHUNK_EXCERPT / 2) {
            cutIndex = MAX_CHARS_PER_CHUNK_EXCERPT - 3;
        }
        return normalized.substring(0, cutIndex) + "...";
    }
}
