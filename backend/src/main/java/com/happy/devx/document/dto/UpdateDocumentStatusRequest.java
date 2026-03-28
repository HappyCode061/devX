package com.happy.devx.document.dto;

import com.happy.devx.document.entity.DocumentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateDocumentStatusRequest(
        @NotNull(message = "status is required")
        DocumentStatus status
) {
}
