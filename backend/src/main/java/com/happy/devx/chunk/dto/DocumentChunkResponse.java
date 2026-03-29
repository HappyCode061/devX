package com.happy.devx.chunk.dto;

import com.happy.devx.chunk.entity.DocumentChunk;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        int charCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static DocumentChunkResponse from(DocumentChunk chunk) {
        return new DocumentChunkResponse(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getCharCount(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}
