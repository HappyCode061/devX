package com.happy.devx.chunk.repository;

import com.happy.devx.chunk.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    long countByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    @Query("""
            select chunk
            from DocumentChunk chunk
            join fetch chunk.document document
            where lower(chunk.content) like lower(concat('%', :query, '%'))
            order by chunk.charCount asc, chunk.chunkIndex asc
            """)
    List<DocumentChunk> searchByContent(@Param("query") String query, Pageable pageable);
}
