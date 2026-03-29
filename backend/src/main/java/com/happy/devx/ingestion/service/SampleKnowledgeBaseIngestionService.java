package com.happy.devx.ingestion.service;

import com.happy.devx.chunk.service.DocumentChunkService;
import com.happy.devx.chunk.dto.RechunkDocumentResponse;
import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.entity.KnowledgeDocument;
import com.happy.devx.document.repository.KnowledgeDocumentRepository;
import com.happy.devx.ingestion.config.IngestionProperties;
import com.happy.devx.ingestion.dto.IngestionScanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleKnowledgeBaseIngestionService {

    private final IngestionProperties ingestionProperties;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final DocumentChunkService documentChunkService;

    @Transactional
    public IngestionScanResponse ingestSampleKnowledgeBase() {
        if (!ingestionProperties.enabled()) {
            log.info("Sample knowledge base ingestion is disabled.");
            return new IngestionScanResponse(0, 0, 0, 0);
        }

        Optional<Path> resolvedPath = resolveKnowledgeBasePath(ingestionProperties.sampleKnowledgeBasePath());
        if (resolvedPath.isEmpty()) {
            log.warn("Sample knowledge base path could not be resolved from configured value '{}'", ingestionProperties.sampleKnowledgeBasePath());
            return new IngestionScanResponse(0, 0, 0, 0);
        }

        Path knowledgeBasePath = resolvedPath.get();
        log.info("Starting sample knowledge base scan at {}", knowledgeBasePath.toAbsolutePath());

        try (Stream<Path> pathStream = Files.walk(knowledgeBasePath)) {
            List<Path> files = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .sorted(Comparator.naturalOrder())
                    .toList();

            int createdCount = 0;
            int skippedCount = 0;
            int chunksCreated = 0;

            for (Path file : files) {
                String sourcePath = toSourcePath(knowledgeBasePath, file);
                KnowledgeDocument document = knowledgeDocumentRepository.findBySourcePath(sourcePath).orElse(null);

                if (document == null) {
                    document = new KnowledgeDocument();
                    document.setTitle(toTitle(file));
                    document.setSourcePath(sourcePath);
                    document.setStatus(DocumentStatus.DISCOVERED);
                    document.setOrigin(DocumentOrigin.SAMPLE_KNOWLEDGE_BASE);
                    document = knowledgeDocumentRepository.save(document);
                    createdCount++;
                    log.info("Registered document: {}", sourcePath);
                } else {
                    skippedCount++;
                    log.info("Skipping already registered document metadata: {}", sourcePath);
                }

                String content = Files.readString(file, StandardCharsets.UTF_8);
                String contentHash = hashContent(content);
                if (document.getContentHash() == null || !document.getContentHash().equals(contentHash) || !documentChunkService.hasChunks(document.getId())) {
                    chunksCreated += documentChunkService.replaceChunksForDocument(document, content);
                    document.setContentHash(contentHash);
                    knowledgeDocumentRepository.save(document);
                    log.info("Synchronized chunks for document: {}", sourcePath);
                } else {
                    log.info("Skipping chunk regeneration because content hash is unchanged for {}", sourcePath);
                }
            }

            log.info(
                    "Sample knowledge base scan complete. filesFound={}, documentsCreated={}, documentsSkipped={}, chunksCreated={}",
                    files.size(), createdCount, skippedCount, chunksCreated
            );
            return new IngestionScanResponse(files.size(), createdCount, skippedCount, chunksCreated);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan sample knowledge base", exception);
        }
    }

    @Transactional
    public RechunkDocumentResponse rechunkSampleKnowledgeBaseDocument(UUID documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Document not found"
                ));

        if (document.getOrigin() != DocumentOrigin.SAMPLE_KNOWLEDGE_BASE) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Manual documents cannot be re-chunked from the sample knowledge base"
            );
        }

        Path filePath = resolveSourcePath(document.getSourcePath())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Source file not found for document"
                ));

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            String contentHash = hashContent(content);
            int chunksCreated = documentChunkService.replaceChunksForDocument(document, content);
            document.setContentHash(contentHash);
            knowledgeDocumentRepository.save(document);
            return new RechunkDocumentResponse(document.getId(), document.getSourcePath(), contentHash, chunksCreated);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to rechunk source document", exception);
        }
    }

    private Optional<Path> resolveKnowledgeBasePath(String configuredPath) {
        Path rawPath = Path.of(configuredPath);
        if (rawPath.isAbsolute() && Files.exists(rawPath)) {
            return Optional.of(rawPath.normalize());
        }

        Path currentDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                rawPath.toAbsolutePath().normalize(),
                currentDirectory.resolve(configuredPath).normalize(),
                currentDirectory.getParent() != null
                        ? currentDirectory.getParent().resolve(configuredPath).normalize()
                        : currentDirectory.resolve(configuredPath).normalize()
        );

        return candidates.stream().filter(Files::exists).findFirst();
    }

    private String toSourcePath(Path knowledgeBasePath, Path file) {
        String relativePath = knowledgeBasePath.relativize(file).toString().replace('\\', '/');
        return knowledgeBasePath.getFileName() + "/" + relativePath;
    }

    private Optional<Path> resolveSourcePath(String sourcePath) {
        Optional<Path> basePath = resolveKnowledgeBasePath(ingestionProperties.sampleKnowledgeBasePath());
        if (basePath.isEmpty()) {
            return Optional.empty();
        }

        String prefix = basePath.get().getFileName() + "/";
        if (!sourcePath.startsWith(prefix)) {
            return Optional.empty();
        }

        String relativePath = sourcePath.substring(prefix.length());
        Path resolved = basePath.get().resolve(relativePath).normalize();
        return Files.exists(resolved) ? Optional.of(resolved) : Optional.empty();
    }

    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private String toTitle(Path file) {
        String filename = file.getFileName().toString();
        int extensionIndex = filename.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
        return Arrays.stream(baseName.split("[-_]"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(baseName);
    }
}
