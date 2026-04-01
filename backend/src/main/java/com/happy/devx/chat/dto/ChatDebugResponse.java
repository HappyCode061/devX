package com.happy.devx.chat.dto;

import java.util.List;

public record ChatDebugResponse(
        String generationMode,
        String promptPreview,
        int promptPreviewLength,
        List<ChatDebugSourceResponse> selectedSources
) {
}
