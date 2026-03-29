ALTER TABLE knowledge_documents
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);
