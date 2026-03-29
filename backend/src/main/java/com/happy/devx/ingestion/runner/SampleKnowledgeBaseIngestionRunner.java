package com.happy.devx.ingestion.runner;

import com.happy.devx.ingestion.dto.IngestionScanResponse;
import com.happy.devx.ingestion.service.SampleKnowledgeBaseIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SampleKnowledgeBaseIngestionRunner implements ApplicationRunner {

    private final SampleKnowledgeBaseIngestionService sampleKnowledgeBaseIngestionService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Triggering sample knowledge base ingestion at startup");
        IngestionScanResponse response = sampleKnowledgeBaseIngestionService.ingestSampleKnowledgeBase();
        log.info(
                "Startup ingestion summary: filesFound={}, documentsCreated={}, documentsSkipped={}, chunksCreated={}",
                response.filesFound(),
                response.documentsCreated(),
                response.documentsSkipped(),
                response.chunksCreated()
        );
    }
}
