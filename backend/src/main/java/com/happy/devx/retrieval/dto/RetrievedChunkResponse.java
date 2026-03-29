package com.happy.devx.retrieval.dto;

import com.happy.devx.chunk.dto.ChunkSearchResponse;

import java.util.UUID;

public record RetrievedChunkResponse(
        UUID chunkId,
        int chunkIndex,
        String content,
        int charCount
) {

    public static RetrievedChunkResponse from(ChunkSearchResponse chunk) {
        return new RetrievedChunkResponse(
                chunk.chunkId(),
                chunk.chunkIndex(),
                chunk.content(),
                chunk.charCount()
        );
    }
}
