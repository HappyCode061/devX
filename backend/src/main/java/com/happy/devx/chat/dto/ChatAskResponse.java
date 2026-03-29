package com.happy.devx.chat.dto;

import java.util.List;

public record ChatAskResponse(
        String question,
        String answer,
        List<String> matchedTerms,
        int matchedDocumentCount,
        int matchedChunkCount,
        List<ChatCitationResponse> citations
) {
}
