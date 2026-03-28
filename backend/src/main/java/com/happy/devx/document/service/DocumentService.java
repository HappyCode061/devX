package com.happy.devx.document.service;

import com.happy.devx.document.dto.CreateDocumentRequest;
import com.happy.devx.document.dto.DocumentResponse;
import com.happy.devx.document.dto.PagedDocumentResponse;
import com.happy.devx.document.dto.UpdateDocumentStatusRequest;
import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.entity.KnowledgeDocument;
import com.happy.devx.document.repository.KnowledgeDocumentRepository;
import com.happy.devx.document.repository.KnowledgeDocumentSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Transactional(readOnly = true)
    public PagedDocumentResponse getDocuments(int page, int size, DocumentOrigin origin, DocumentStatus status) {
        log.info("Fetching document list page={} size={} origin={} status={}", page, size, origin, status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Specification<KnowledgeDocument> specification = Specification
                .where(KnowledgeDocumentSpecifications.hasOrigin(origin))
                .and(KnowledgeDocumentSpecifications.hasStatus(status));
        return PagedDocumentResponse.from(
                knowledgeDocumentRepository.findAll(specification, pageable)
                        .map(DocumentResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID id) {
        log.info("Fetching document id={}", id);
        return knowledgeDocumentRepository.findById(id)
                .map(DocumentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
    }

    @Transactional
    public DocumentResponse createDocument(CreateDocumentRequest request) {
        String normalizedTitle = request.title().trim();
        String normalizedSourcePath = request.sourcePath().trim();

        log.info("Creating document metadata for sourcePath={}", normalizedSourcePath);

        if (knowledgeDocumentRepository.existsBySourcePath(normalizedSourcePath)) {
            log.warn("Document creation rejected because sourcePath already exists: {}", normalizedSourcePath);
            throw new ResponseStatusException(CONFLICT, "A document with this sourcePath already exists");
        }

        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(normalizedTitle);
        document.setSourcePath(normalizedSourcePath);
        document.setStatus(DocumentStatus.DISCOVERED);
        document.setOrigin(DocumentOrigin.MANUAL);

        DocumentResponse savedDocument = DocumentResponse.from(knowledgeDocumentRepository.save(document));
        log.info("Created document id={} sourcePath={}", savedDocument.id(), savedDocument.sourcePath());
        return savedDocument;
    }

    @Transactional
    public DocumentResponse updateDocumentStatus(UUID id, UpdateDocumentStatusRequest request) {
        log.info("Updating document status id={} status={}", id, request.status());

        KnowledgeDocument document = knowledgeDocumentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));

        document.setStatus(request.status());

        DocumentResponse updatedDocument = DocumentResponse.from(knowledgeDocumentRepository.save(document));
        log.info("Updated document status id={} status={}", updatedDocument.id(), updatedDocument.status());
        return updatedDocument;
    }
}
