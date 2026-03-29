package com.happy.devx.chunk.service;

import com.happy.devx.chunk.config.ChunkingProperties;
import com.happy.devx.chunk.entity.DocumentChunk;
import com.happy.devx.chunk.repository.DocumentChunkRepository;
import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.entity.KnowledgeDocument;
import com.happy.devx.document.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentChunkServiceTest {

    @Mock
    private ChunkingProperties chunkingProperties;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @InjectMocks
    private DocumentChunkService documentChunkService;

    @Test
    void replaceChunksFlushesDeleteBeforeSavingNewChunks() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        document.setTitle("Incident Runbook");
        document.setSourcePath("sample-knowledge-base/incident-runbook.md");
        document.setStatus(DocumentStatus.DISCOVERED);
        document.setOrigin(DocumentOrigin.SAMPLE_KNOWLEDGE_BASE);

        given(chunkingProperties.enabled()).willReturn(true);
        given(chunkingProperties.targetSize()).willReturn(50);
        given(chunkingProperties.overlapSize()).willReturn(10);

        int created = documentChunkService.replaceChunksForDocument(
                document,
                "PostgreSQL checks should happen first.\n\nFlyway migrations should be verified next."
        );

        assertThat(created).isGreaterThan(0);

        InOrder inOrder = inOrder(documentChunkRepository);
        inOrder.verify(documentChunkRepository).deleteByDocumentId(document.getId());
        inOrder.verify(documentChunkRepository).flush();
        inOrder.verify(documentChunkRepository).saveAll(anyList());

        ArgumentCaptor<List<DocumentChunk>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isNotEmpty();
        assertThat(captor.getValue())
                .extracting(DocumentChunk::getChunkIndex)
                .containsExactly(0, 1);
    }
}
