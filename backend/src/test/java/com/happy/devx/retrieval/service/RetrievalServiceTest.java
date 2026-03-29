package com.happy.devx.retrieval.service;

import com.happy.devx.chunk.dto.ChunkSearchResponse;
import com.happy.devx.chunk.service.DocumentChunkService;
import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private DocumentChunkService documentChunkService;

    @InjectMocks
    private RetrievalService retrievalService;

    @Test
    void ranksDocumentsByMatchedChunkCountThenEarliestChunkIndex() {
        ChunkSearchResponse onboardingChunk0 = new ChunkSearchResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",
                0,
                "Start PostgreSQL with Docker Compose.",
                36
        );
        ChunkSearchResponse onboardingChunk2 = new ChunkSearchResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "Onboarding Guide",
                "sample-knowledge-base/onboarding-guide.md",
                2,
                "Verify PostgreSQL connectivity.",
                31
        );
        ChunkSearchResponse runbookChunk1 = new ChunkSearchResponse(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                "Incident Runbook",
                "sample-knowledge-base/incident-runbook.md",
                1,
                "Confirm PostgreSQL is running.",
                29
        );

        given(documentChunkService.searchChunks("postgresql", 5))
                .willReturn(List.of(runbookChunk1, onboardingChunk0, onboardingChunk2));

        RetrievalSearchResponse response = retrievalService.search("postgresql", 5);

        assertThat(response.documents()).hasSize(2);
        assertThat(response.documents().getFirst().documentTitle()).isEqualTo("Onboarding Guide");
        assertThat(response.documents().getFirst().matchedChunkCount()).isEqualTo(2);
        assertThat(response.documents().get(1).documentTitle()).isEqualTo("Incident Runbook");
    }
}
