package com.happy.devx.document.repository;

import com.happy.devx.document.entity.DocumentOrigin;
import com.happy.devx.document.entity.DocumentStatus;
import com.happy.devx.document.entity.KnowledgeDocument;
import org.springframework.data.jpa.domain.Specification;

public final class KnowledgeDocumentSpecifications {

    private KnowledgeDocumentSpecifications() {
    }

    public static Specification<KnowledgeDocument> hasOrigin(DocumentOrigin origin) {
        return (root, query, cb) -> origin == null ? cb.conjunction() : cb.equal(root.get("origin"), origin);
    }

    public static Specification<KnowledgeDocument> hasStatus(DocumentStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
}
