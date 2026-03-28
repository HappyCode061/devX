package com.happy.devx.document.repository;

import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID>, JpaSpecificationExecutor<KnowledgeDocument> {

    Optional<KnowledgeDocument> findBySourcePath(String sourcePath);

    boolean existsBySourcePath(String sourcePath);

    long countByOrigin(DocumentOrigin origin);
}
