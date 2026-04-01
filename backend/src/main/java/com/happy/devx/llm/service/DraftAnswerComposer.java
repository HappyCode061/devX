package com.happy.devx.llm.service;

import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.dto.RetrievedDocumentResponse;
import org.springframework.stereotype.Component;

@Component
public class DraftAnswerComposer {

    private static final int MAX_DOCUMENTS_IN_ANSWER = 3;
    private static final int MAX_CHUNKS_PER_DOCUMENT_IN_ANSWER = 1;
    private static final int MAX_CHARS_PER_CHUNK_EXCERPT = 240;
    private static final String NL = "\n";

    /**
     * Builds a deterministic grounded draft answer from retrieval output.
     */
    public String composeDraftAnswer(String question, RetrievalSearchResponse retrieval) {
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
                    builder.append("- ").append(document.documentTitle()).append(": ");

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
