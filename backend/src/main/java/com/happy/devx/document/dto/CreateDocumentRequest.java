package com.happy.devx.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,
        @NotBlank(message = "sourcePath is required")
        @Size(max = 1000, message = "sourcePath must be at most 1000 characters")
        String sourcePath
) {
}
