# Retrieval Design Notes

## Why We Chunk Documents

Large documents are difficult to search and cite as a single block of text. Chunking creates smaller retrieval units that preserve enough local context while remaining specific enough to rank and cite later. Overlap between chunks reduces the chance that an important sentence falls directly on a boundary.

## Current Strategy

The current implementation uses character-based chunking with a target size and an overlap window. It attempts to break on paragraph boundaries when possible. This is a practical Phase 2 strategy because it is simple, deterministic, and easy to debug with logs and direct database inspection.

## Current Retrieval Behavior

The first retrieval step uses keyword matching against stored chunk content in PostgreSQL. This is intentionally not semantic search. It allows the team to validate ingestion quality, chunk boundaries, and source attribution before introducing embeddings, vector databases, or model-based ranking.

## Future Direction

Once keyword retrieval is stable, the next steps are to store embeddings, rank chunks by semantic similarity, and compose grounded chat responses with citations. At that stage, the retrieval layer should still preserve links back to the originating document, source path, and chunk index so that answers remain auditable.
