package com.happy.devx.chunk.dto;

import java.util.UUID;

public record RechunkDocumentResponse(
        UUID documentId,
        String sourcePath,
        String contentHash,
        int chunksCreated
) {
}
