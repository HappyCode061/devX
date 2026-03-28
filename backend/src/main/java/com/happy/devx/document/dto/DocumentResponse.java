package com.happy.devx.document.dto;

import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.entity.KnowledgeDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String title,
        String sourcePath,
        DocumentStatus status,
        DocumentOrigin origin,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static DocumentResponse from(KnowledgeDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getSourcePath(),
                document.getStatus(),
                document.getOrigin(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
