package com.happy.devx.retrieval.dto;

import java.util.List;

public record RetrievalSearchResponse(
        String query,
        List<String> matchedTerms,
        int requestedLimit,
        int matchedChunkCount,
        int matchedDocumentCount,
        List<RetrievedDocumentResponse> documents
) {
}
