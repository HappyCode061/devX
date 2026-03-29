package com.happy.devx.retrieval.service;

import com.happy.devx.chunk.dto.ChunkSearchResponse;
import com.happy.devx.chunk.service.DocumentChunkService;
import com.happy.devx.retrieval.dto.RetrievalSearchResponse;
import com.happy.devx.retrieval.dto.RetrievedDocumentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetrievalService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "do", "for", "how", "i", "in", "is", "of", "on", "or", "the", "to", "we", "what"
    );

    private final DocumentChunkService documentChunkService;

    /**
     * Expands a natural-language query into retrieval terms, searches chunks, and ranks matched documents.
     */
    @Transactional(readOnly = true)
    public RetrievalSearchResponse search(String query, int limit) {
        log.info("Retrieval search query='{}' limit={}", query, limit);

        List<String> searchTerms = extractSearchTerms(query);
        log.info("Retrieval normalized terms={}", searchTerms);

        Map<UUID, ChunkSearchResponse> uniqueChunkMatches = new LinkedHashMap<>();
        Map<UUID, LinkedHashSet<String>> matchedTermsByDocument = new LinkedHashMap<>();
        for (String term : searchTerms) {
            List<ChunkSearchResponse> termMatches = documentChunkService.searchChunks(term, limit);
            for (ChunkSearchResponse match : termMatches) {
                uniqueChunkMatches.putIfAbsent(match.chunkId(), match);
                matchedTermsByDocument
                        .computeIfAbsent(match.documentId(), ignored -> new LinkedHashSet<>())
                        .add(term);
                if (uniqueChunkMatches.size() >= limit) {
                    break;
                }
            }
            if (uniqueChunkMatches.size() >= limit) {
                break;
            }
        }

        List<ChunkSearchResponse> chunkMatches = new ArrayList<>(uniqueChunkMatches.values());
        Map<UUID, List<ChunkSearchResponse>> chunksByDocument = new LinkedHashMap<>();

        for (ChunkSearchResponse chunk : chunkMatches) {
            chunksByDocument.computeIfAbsent(chunk.documentId(), ignored -> new java.util.ArrayList<>()).add(chunk);
        }

        List<RetrievedDocumentResponse> documents = chunksByDocument.entrySet()
                .stream()
                .map(entry -> RetrievedDocumentResponse.from(
                        entry.getKey(),
                        entry.getValue(),
                        matchedTermsByDocument.getOrDefault(entry.getKey(), new LinkedHashSet<>()).stream().toList()
                ))
                .sorted(Comparator
                        .comparingInt(RetrievedDocumentResponse::matchedChunkCount).reversed()
                        .thenComparingInt(this::earliestChunkIndex)
                        .thenComparing(RetrievedDocumentResponse::documentTitle))
                .toList();

        return new RetrievalSearchResponse(
                query,
                searchTerms,
                limit,
                chunkMatches.size(),
                documents.size(),
                documents
        );
    }

    /**
     * Extracts useful retrieval terms from a natural-language question.
     */
    private List<String> extractSearchTerms(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of("");
        }

        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(normalized);

        for (String token : normalized.split("[^a-z0-9]+")) {
            if (token.isBlank() || STOP_WORDS.contains(token) || token.length() < 3) {
                continue;
            }
            terms.add(token);
            terms.addAll(expandAliases(token));
        }

        return terms.stream().toList();
    }

    /**
     * Adds simple synonyms so common engineering shorthand retrieves better results.
     */
    private List<String> expandAliases(String token) {
        return switch (token) {
            case "postgres", "postgresql" -> List.of("postgres", "postgresql", "database");
            case "auth", "authentication" -> List.of("auth", "authentication", "security");
            case "setup", "configure", "configuration" -> List.of("setup", "configure", "configuration");
            case "logs", "logging" -> List.of("logs", "logging");
            default -> List.of();
        };
    }

    /**
     * Prefers documents whose relevant content appears earlier in the source.
     */
    private int earliestChunkIndex(RetrievedDocumentResponse document) {
        return document.matchedChunks().stream()
                .mapToInt(chunk -> chunk.chunkIndex())
                .min()
                .orElse(Integer.MAX_VALUE);
    }
}
