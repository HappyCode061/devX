package com.happy.devx.retrieval.dto;

import com.happy.devx.chunk.dto.ChunkSearchResponse;

import java.util.List;
import java.util.UUID;

public record RetrievedDocumentResponse(
        UUID documentId,
        String documentTitle,
        String documentSourcePath,
        List<String> matchedTerms,
        int matchedChunkCount,
        List<RetrievedChunkResponse> matchedChunks
) {

    public static RetrievedDocumentResponse from(UUID documentId, List<ChunkSearchResponse> chunks, List<String> matchedTerms) {
        ChunkSearchResponse first = chunks.getFirst();
        return new RetrievedDocumentResponse(
                documentId,
                first.documentTitle(),
                first.documentSourcePath(),
                matchedTerms,
                chunks.size(),
                chunks.stream().map(RetrievedChunkResponse::from).toList()
        );
    }
}
