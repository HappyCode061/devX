package com.happy.devx.ingestion.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleKnowledgeBaseIngestionService {

    private final IngestionProperties ingestionProperties;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Transactional
    public IngestionScanResponse ingestSampleKnowledgeBase() {
        if (!ingestionProperties.enabled()) {
            log.info("Sample knowledge base ingestion is disabled.");
            return new IngestionScanResponse(0, 0, 0);
        }

        Optional<Path> resolvedPath = resolveKnowledgeBasePath(ingestionProperties.sampleKnowledgeBasePath());
        if (resolvedPath.isEmpty()) {
            log.warn(
                    "Sample knowledge base path could not be resolved from configured value '{}'",
                    ingestionProperties.sampleKnowledgeBasePath()
            );
            return new IngestionScanResponse(0, 0, 0);
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
            for (Path file : files) {
                String sourcePath = toSourcePath(knowledgeBasePath, file);
                if (knowledgeDocumentRepository.existsBySourcePath(sourcePath)) {
                    log.info("Skipping already registered document: {}", sourcePath);
                    skippedCount++;
                    continue;
                }

                KnowledgeDocument document = new KnowledgeDocument();
                document.setTitle(toTitle(file));
                document.setSourcePath(sourcePath);
                document.setStatus(DocumentStatus.DISCOVERED);
                document.setOrigin(DocumentOrigin.SAMPLE_KNOWLEDGE_BASE);
                knowledgeDocumentRepository.save(document);
                createdCount++;

                log.info("Registered document: {}", sourcePath);
            }

            log.info(
                    "Sample knowledge base scan complete. filesFound={}, documentsCreated={}, documentsSkipped={}",
                    files.size(),
                    createdCount,
                    skippedCount
            );
            return new IngestionScanResponse(files.size(), createdCount, skippedCount);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan sample knowledge base", exception);
        }
    }

    private String toSourcePath(Path knowledgeBasePath, Path file) {
        String relativePath = knowledgeBasePath.relativize(file).toString().replace('\\', '/');
        return knowledgeBasePath.getFileName() + "/" + relativePath;
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

        return candidates.stream()
                .filter(Files::exists)
                .findFirst();
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
