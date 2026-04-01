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
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
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
                        sortChunksForDocument(entry.getValue(), searchTerms, normalizedQuery),
                        matchedTermsByDocument.getOrDefault(entry.getKey(), new LinkedHashSet<>()).stream().toList()
                ))
                .sorted(Comparator
                        .comparingInt((RetrievedDocumentResponse document) -> titleAndPathMatches(document, searchTerms)).reversed()
                        .thenComparingDouble(document -> termCoverage(document, searchTerms)).reversed()
                        .thenComparingInt((RetrievedDocumentResponse document) -> containsExactPhrase(document, normalizedQuery) ? 1 : 0).reversed()
                        .thenComparingInt(RetrievedDocumentResponse::matchedChunkCount).reversed()
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

    private List<ChunkSearchResponse> sortChunksForDocument(
            List<ChunkSearchResponse> chunks,
            List<String> searchTerms,
            String normalizedQuery
    ) {
        return chunks.stream()
                .sorted(Comparator
                        .comparingInt((ChunkSearchResponse chunk) -> countMatchingTerms(chunk.content(), searchTerms)).reversed()
                        .thenComparingInt(chunk -> containsIgnoreCase(chunk.content(), normalizedQuery) ? 1 : 0).reversed()
                        .thenComparingInt(ChunkSearchResponse::chunkIndex))
                .toList();
    }

    private int titleAndPathMatches(RetrievedDocumentResponse document, List<String> searchTerms) {
        String title = document.documentTitle().toLowerCase();
        String path = document.documentSourcePath().toLowerCase();
        int matches = 0;
        for (String term : searchTerms) {
            if (term.isBlank()) {
                continue;
            }
            if (title.contains(term) || path.contains(term)) {
                matches++;
            }
        }
        return matches;
    }

    private double termCoverage(RetrievedDocumentResponse document, List<String> searchTerms) {
        long meaningfulTerms = searchTerms.stream().filter(term -> !term.isBlank()).count();
        if (meaningfulTerms == 0) {
            return 0;
        }
        return (double) document.matchedTerms().size() / meaningfulTerms;
    }

    private boolean containsExactPhrase(RetrievedDocumentResponse document, String normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            return false;
        }
        return document.matchedChunks().stream()
                .anyMatch(chunk -> containsIgnoreCase(chunk.content(), normalizedQuery));
    }

    private int countMatchingTerms(String content, List<String> searchTerms) {
        int matches = 0;
        for (String term : searchTerms) {
            if (!term.isBlank() && containsIgnoreCase(content, term)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean containsIgnoreCase(String content, String term) {
        return content != null && term != null && !term.isBlank() && content.toLowerCase().contains(term.toLowerCase());
    }
}
