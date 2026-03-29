# devX Enterprise Assistant

The current setup includes the Phase 1 backend foundation and the Phase 2 retrieval pipeline for the DevX Enterprise Assistant. The backend can register documents, ingest sample knowledge-base files, generate chunks, search and rank retrieval results, and return grounded draft chat responses with citations.

## Repository Layout

```text
backend/
docs/
sample-knowledge-base/
vscode-extension/
.github/workflows/
```

## Backend

The `backend/` directory contains a Spring Boot 3 application using Java 21 and Maven.

### Included backend foundation

- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL driver
- Flyway
- Actuator
- Lombok
- Validation

### Current package structure

```text
com.happy.devx
  common/      # health checks and global API error handling
  config/      # security and OpenAPI configuration
  document/    # document metadata domain, APIs, services, repositories
  ingestion/   # sample knowledge-base scanning and processing workflow
  chunk/       # retrieval-sized content units, chunk APIs, chunk services
  retrieval/   # query-time retrieval and ranking
  chat/        # grounded draft answer composition and chat API
```

### Run locally

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

Then open [http://localhost:8080/api/health](http://localhost:8080/api/health).

### Sample knowledge base

The local sample knowledge base currently includes:

- `sample-knowledge-base/onboarding-guide.md`
- `sample-knowledge-base/api-standards.md`
- `sample-knowledge-base/security-auth-guide.md`
- `sample-knowledge-base/incident-runbook.md`
- `sample-knowledge-base/retrieval-design-notes.md`

### Current API Surface

- `GET /api/health`
- `GET /api/documents?page=0&size=20`
- `GET /api/documents/{id}`
- `GET /api/documents/{id}/chunks`
- `POST /api/documents`
- `POST /api/documents/{id}/rechunk`
- `PATCH /api/documents/{id}/status`
- `POST /api/ingestion/scan`
- `GET /api/chunks/search?query=...&limit=...`
- `GET /api/retrieval/search?query=...&limit=...`
- `POST /api/chat/ask`

### Example workflow

1. Start PostgreSQL and the backend
2. Trigger ingestion:

```bash
curl -X POST http://localhost:8080/api/ingestion/scan
```

3. List ingested documents:

```bash
curl "http://localhost:8080/api/documents?page=0&size=20"
```

4. Search chunks directly:

```bash
curl "http://localhost:8080/api/chunks/search?query=postgresql&limit=5"
```

5. Run grouped retrieval:

```bash
curl "http://localhost:8080/api/retrieval/search?query=how%20to%20setup%20postgres&limit=5"
```

6. Ask the grounded draft chat endpoint:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "how to setup postgres",
    "retrievalLimit": 5
  }'
```

### Swagger UI

When the app is running, open:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Current system behavior

- `sample-knowledge-base/` is a local demo source, not the final production storage strategy.
- Postgres currently stores document metadata, lifecycle information, and chunks.
- `origin` distinguishes manual registrations from sample knowledge base discoveries.
- `status` tracks the document lifecycle: `DISCOVERED`, `INGESTED`, `FAILED`.
- Chunking is character-based with overlap and paragraph-aware boundaries when possible.
- Retrieval currently uses keyword-based matching, simple query normalization, and document ranking.
- Chat currently returns a retrieval-backed draft answer with citations; it is not yet a real LLM-generated answer.

### Phase status

Phase 1 complete:
- backend skeleton
- document metadata
- ingestion scaffolding
- health, docs, and error handling

Phase 2 complete:
- chunk generation
- rechunking and content-hash refresh logic
- chunk search
- retrieval grouping and ranking
- grounded draft chat flow

Phase 3 deferred:
- real LLM integration
- prompt construction
- semantic retrieval / embeddings
- answer generation with model-backed reasoning
