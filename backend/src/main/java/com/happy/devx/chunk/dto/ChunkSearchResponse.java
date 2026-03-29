package com.happy.devx.chunk.dto;

import com.happy.devx.chunk.entity.DocumentChunk;

import java.util.UUID;

public record ChunkSearchResponse(
        UUID chunkId,
        UUID documentId,
        String documentTitle,
        String documentSourcePath,
        int chunkIndex,
        String content,
        int charCount
) {

    public static ChunkSearchResponse from(DocumentChunk chunk) {
        return new ChunkSearchResponse(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getDocument().getTitle(),
                chunk.getDocument().getSourcePath(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                chunk.getCharCount()
        );
    }
}
