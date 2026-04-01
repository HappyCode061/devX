# devX Enterprise Assistant

The current setup includes the Phase 1 backend foundation, the Phase 2 retrieval pipeline, and the Phase 3 provider-extensible LLM integration layer for the DevX Enterprise Assistant. The backend can register documents, ingest sample knowledge-base files, generate chunks, search and rank retrieval results, and return grounded chat responses with citations through either a fallback draft mode or a real LLM provider.

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

### Environment configuration

Copy values from `.env.example` into your local environment as needed:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/devx_assistant
export DB_USERNAME=devx
export DB_PASSWORD=devx
export LLM_API_KEY=your_api_key_here
```

The repository defaults are safe:

- `devx.llm.enabled=false`
- `devx.llm.provider=draft`
- `devx.llm.model=retrieval-draft-v1`

That means the app starts in fallback draft mode unless you explicitly enable a real provider.

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

6. Ask the grounded chat endpoint:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "how to setup postgres",
    "retrievalLimit": 5,
    "includeDebug": false
  }'
```

### Dual-terminal comparison workflow

Run one instance in fallback mode:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080 --devx.llm.enabled=false"
```

Run a second instance in OpenAI mode:

```bash
cd backend
LLM_API_KEY=your_api_key_here mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081 --devx.llm.enabled=true --devx.llm.provider=openai --devx.llm.model=gpt-5-nano --devx.llm.base-url=https://api.openai.com/v1"
```

Compare the two with debug enabled:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "how to setup postgres",
    "retrievalLimit": 5,
    "includeDebug": true
  }'
```

```bash
curl -X POST http://localhost:8081/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "how to setup postgres",
    "retrievalLimit": 5,
    "includeDebug": true
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
- Chat can now run in two modes:
  - fallback draft mode
  - provider-backed mode through the extensible LLM layer
- Debug mode can optionally expose the selected sources and prompt preview for A/B testing.

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

Phase 3 complete:
- provider-extensible LLM abstraction
- OpenAI provider integration
- prompt builder and context budgeting
- debug prompt preview for comparison testing
- provider-aware chat response metadata

Phase 3 follow-up candidates:
- stronger context scoring
- prompt/citation alignment refinement
- semantic retrieval / embeddings
- second provider integration
