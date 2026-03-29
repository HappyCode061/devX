package com.happy.devx.ingestion.dto;

public record IngestionScanResponse(
        int filesFound,
        int documentsCreated,
        int documentsSkipped,
        int chunksCreated
) {
}
