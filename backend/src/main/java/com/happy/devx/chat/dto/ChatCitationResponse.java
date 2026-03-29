package com.happy.devx.chat.dto;

import java.util.UUID;

public record ChatCitationResponse(
        UUID documentId,
        String documentTitle,
        String documentSourcePath,
        UUID chunkId,
        int chunkIndex
) {
}
