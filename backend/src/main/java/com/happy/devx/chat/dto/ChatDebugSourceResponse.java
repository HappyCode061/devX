package com.happy.devx.chat.dto;

import java.util.List;
import java.util.UUID;

public record ChatDebugSourceResponse(
        UUID documentId,
        String documentTitle,
        String documentSourcePath,
        List<String> matchedTerms,
        List<Integer> chunkIndexes
) {
}
