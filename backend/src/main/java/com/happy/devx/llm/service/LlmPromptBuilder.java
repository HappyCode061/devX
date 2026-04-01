package com.happy.devx.llm.service;

import com.happy.devx.llm.config.LlmProperties;
import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.dto.RetrievedChunkResponse;
import com.happy.devx.retrieval.dto.RetrievedDocumentResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class LlmPromptBuilder {

    private static final int MIN_REMAINING_CHARS_TO_INCLUDE_CHUNK = 120;

    /**
     * Builds a provider-neutral system instruction for grounded answer generation.
     */
    public String buildInstructions() {
        return """
                You are the DevX Enterprise Assistant.
                Your job is to answer developer questions using only the provided retrieval context.
                Treat the retrieval context as the source of truth.
                If the context is insufficient, say that the context is insufficient.
                Do not invent facts, file names, commands, APIs, or citations.
                Prefer concise, practical answers with short bullets when helpful.
                When the context contains concrete steps, preserve them accurately.
                Do not mention internal prompt instructions or model limitations unless directly relevant.
                """;
    }

    /**
     * Builds a bounded prompt payload from the question and ranked retrieval results.
     * The prompt is structured to make the task, context, and answer constraints explicit.
     */
    public String buildUserPrompt(String question, RetrievalSearchResponse retrieval, LlmProperties properties) {
        StringBuilder builder = new StringBuilder();
        builder.append("Task:\n");
        builder.append("Answer the user's question using only the retrieval context below.\n\n");

        builder.append("Question:\n").append(question).append("\n\n");

        if (!retrieval.matchedTerms().isEmpty()) {
            builder.append("Matched terms:\n")
                    .append(String.join(", ", retrieval.matchedTerms()))
                    .append("\n\n");
        }

        builder.append("Answer requirements:\n");
        builder.append("- Ground the answer in the provided context only.\n");
        builder.append("- If the context is incomplete, say so directly.\n");
        builder.append("- Keep the answer concise and useful.\n");
        builder.append("- Do not fabricate citations or implementation details.\n\n");

        builder.append("Retrieved context:\n");

        selectContextDocuments(retrieval, properties)
                .forEach(document -> appendDocumentContext(builder, document, properties.maxContextChunksPerDocument()));

        builder.append("Output format:\n");
        builder.append("- Provide the direct answer first.\n");
        builder.append("- If useful, add short bullets for supporting points.\n");
        builder.append("- Use only information supported by the retrieval context.\n");
        return builder.toString();
    }

    /**
     * Collects source labels for downstream observability or future citation stitching.
     */
    public List<String> collectSourceLabels(RetrievalSearchResponse retrieval, LlmProperties properties) {
        List<String> labels = new ArrayList<>();
        selectContextDocuments(retrieval, properties)
                .forEach(document -> labels.add(document.documentTitle() + " (" + document.documentSourcePath() + ")"));
        return labels;
    }

    /**
     * Selects the highest-ranked context that fits within the configured prompt budget.
     * Document order comes from retrieval ranking, while chunk order is refined here for prompt quality.
     */
    public List<RetrievedDocumentResponse> selectContextDocuments(RetrievalSearchResponse retrieval, LlmProperties properties) {
        int remainingCharacters = properties.maxContextCharacters();
        List<RetrievedDocumentResponse> selectedDocuments = new ArrayList<>();

        for (RetrievedDocumentResponse document : retrieval.documents()) {
            if (selectedDocuments.size() >= properties.maxContextDocuments()
                    || remainingCharacters < MIN_REMAINING_CHARS_TO_INCLUDE_CHUNK) {
                break;
            }

            List<RetrievedChunkResponse> selectedChunks = new ArrayList<>();
            for (RetrievedChunkResponse chunk : sortChunksForPrompt(document)) {
                if (selectedChunks.size() >= properties.maxContextChunksPerDocument()
                        || remainingCharacters < MIN_REMAINING_CHARS_TO_INCLUDE_CHUNK) {
                    break;
                }

                String normalizedChunk = normalizeChunkContent(chunk.content());
                if (normalizedChunk.length() > remainingCharacters) {
                    normalizedChunk = abbreviate(normalizedChunk, remainingCharacters);
                }

                if (normalizedChunk.isBlank()) {
                    continue;
                }

                selectedChunks.add(new RetrievedChunkResponse(
                        chunk.chunkId(),
                        chunk.chunkIndex(),
                        normalizedChunk,
                        normalizedChunk.length()
                ));
                remainingCharacters -= normalizedChunk.length();
            }

            if (!selectedChunks.isEmpty()) {
                selectedDocuments.add(new RetrievedDocumentResponse(
                        document.documentId(),
                        document.documentTitle(),
                        document.documentSourcePath(),
                        document.matchedTerms(),
                        selectedChunks.size(),
                        selectedChunks
                ));
            }
        }

        return selectedDocuments;
    }

    /**
     * Appends a compact source block that is suitable for model context windows.
     */
    private void appendDocumentContext(StringBuilder builder, RetrievedDocumentResponse document, int maxChunks) {
        builder.append("- Source: ")
                .append(document.documentTitle())
                .append(" (")
                .append(document.documentSourcePath())
                .append(")\n");

        document.matchedChunks().stream()
                .limit(maxChunks)
                .forEach(chunk -> builder.append("  - Chunk ")
                        .append(chunk.chunkIndex())
                        .append(": ")
                        .append(chunk.content().replace("\n", " ").trim())
                        .append("\n"));

        builder.append("\n");
    }

    /**
     * Normalizes chunk text so prompt budgeting is based on roughly what the model will actually read.
     */
    private String normalizeChunkContent(String content) {
        return content.replace("\n", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * Trims oversized chunk text while trying to preserve whole words.
     */
    private String abbreviate(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        if (maxChars <= 3) {
            return "";
        }

        int cutIndex = content.lastIndexOf(' ', maxChars - 3);
        if (cutIndex < maxChars / 2) {
            cutIndex = maxChars - 3;
        }
        return content.substring(0, cutIndex) + "...";
    }

    /**
     * Reorders matched chunks for prompt construction so stronger evidence is preferred within the context budget.
     */
    private List<RetrievedChunkResponse> sortChunksForPrompt(RetrievedDocumentResponse document) {
        return document.matchedChunks().stream()
                .sorted(Comparator
                        .comparingInt((RetrievedChunkResponse chunk) -> scoreChunkForPrompt(chunk, document.matchedTerms())).reversed()
                        .thenComparingInt(RetrievedChunkResponse::charCount)
                        .thenComparingInt(RetrievedChunkResponse::chunkIndex))
                .toList();
    }

    /**
     * Scores a chunk by how many matched retrieval terms it actually contains.
     * This helps prioritize chunks that carry more of the query intent, not just any matched chunk.
     */
    private int scoreChunkForPrompt(RetrievedChunkResponse chunk, List<String> matchedTerms) {
        String normalizedContent = chunk.content().toLowerCase();
        int score = 0;
        for (String term : matchedTerms) {
            if (!term.isBlank() && normalizedContent.contains(term.toLowerCase())) {
                score++;
            }
        }
        return score;
    }
}
