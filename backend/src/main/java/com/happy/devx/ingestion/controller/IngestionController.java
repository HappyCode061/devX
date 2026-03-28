package com.happy.devx.ingestion.controller;

import com.happy.devx.ingestion.dto.IngestionScanResponse;
import com.happy.devx.ingestion.service.SampleKnowledgeBaseIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final SampleKnowledgeBaseIngestionService sampleKnowledgeBaseIngestionService;

    @PostMapping("/scan")
    public IngestionScanResponse scanSampleKnowledgeBase() {
        return sampleKnowledgeBaseIngestionService.ingestSampleKnowledgeBase();
    }
}
