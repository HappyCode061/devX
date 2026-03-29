package com.happy.devx.chunk.service;

import com.happy.devx.chunk.config.ChunkingProperties;
import com.happy.devx.chunk.dto.ChunkSearchResponse;
import com.happy.devx.chunk.dto.DocumentChunkResponse;
import com.happy.devx.chunk.entity.DocumentChunk;
import com.happy.devx.chunk.repository.DocumentChunkRepository;
import com.happy.devx.document.entity.KnowledgeDocument;
import com.happy.devx.document.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentChunkService {

    private final ChunkingProperties chunkingProperties;
    private final DocumentChunkRepository documentChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getChunksForDocument(UUID documentId) {
        if (!knowledgeDocumentRepository.existsById(documentId)) {
            throw new ResponseStatusException(NOT_FOUND, "Document not found");
        }

        return documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId)
                .stream()
                .map(DocumentChunkResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChunkSearchResponse> searchChunks(String query, int limit) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "query is required");
        }

        log.info("Searching chunks query='{}' limit={}", normalizedQuery, limit);
        return documentChunkRepository.searchByContent(normalizedQuery, PageRequest.of(0, limit))
                .stream()
                .map(ChunkSearchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasChunks(UUID documentId) {
        return documentChunkRepository.countByDocumentId(documentId) > 0;
    }

    @Transactional
    public int generateChunksForDocument(KnowledgeDocument document, String content) {
        if (!chunkingProperties.enabled()) {
            log.info("Chunk generation is disabled for document id={}", document.getId());
            return 0;
        }

        if (content == null || content.isBlank()) {
            log.info("Skipping chunk generation for empty document id={}", document.getId());
            return 0;
        }

        if (documentChunkRepository.countByDocumentId(document.getId()) > 0) {
            log.info("Skipping chunk generation because chunks already exist for document id={}", document.getId());
            return 0;
        }

        return saveChunks(document, content);
    }

    @Transactional
    public int replaceChunksForDocument(KnowledgeDocument document, String content) {
        documentChunkRepository.deleteByDocumentId(document.getId());
        documentChunkRepository.flush();
        log.info("Deleted existing chunks for document id={}", document.getId());
        return saveChunks(document, content);
    }

    private int saveChunks(KnowledgeDocument document, String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }

        List<String> chunks = splitIntoChunks(content, chunkingProperties.targetSize(), chunkingProperties.overlapSize());
        List<DocumentChunk> entities = new ArrayList<>();

        for (int index = 0; index < chunks.size(); index++) {
            String chunkContent = chunks.get(index);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(index);
            chunk.setContent(chunkContent);
            chunk.setCharCount(chunkContent.length());
            entities.add(chunk);
        }

        documentChunkRepository.saveAll(entities);
        log.info("Generated {} chunks for document id={}", entities.size(), document.getId());
        return entities.size();
    }

    private List<String> splitIntoChunks(String content, int targetSize, int overlapSize) {
        String normalized = content.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + targetSize, normalized.length());
            if (end < normalized.length()) {
                int paragraphBreak = normalized.lastIndexOf("\n\n", end);
                if (paragraphBreak > start + (targetSize / 2)) {
                    end = paragraphBreak;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(end - overlapSize, start + 1);
        }

        return chunks;
    }
}
