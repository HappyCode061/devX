package com.happy.devx.chat.dto;

import java.util.List;

public record ChatAskResponse(
        String question,
        String answer,
        String provider,
        String model,
        boolean fallbackUsed,
        List<String> matchedTerms,
        int matchedDocumentCount,
        int matchedChunkCount,
        List<ChatCitationResponse> citations,
        ChatDebugResponse debug
) {
}
