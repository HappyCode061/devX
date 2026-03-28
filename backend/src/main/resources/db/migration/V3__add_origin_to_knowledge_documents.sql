ALTER TABLE knowledge_documents
    ADD COLUMN IF NOT EXISTS origin VARCHAR(50);

UPDATE knowledge_documents
SET origin = 'MANUAL'
WHERE origin IS NULL;

ALTER TABLE knowledge_documents
    ALTER COLUMN origin SET NOT NULL;
