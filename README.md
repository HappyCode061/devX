# devX Enterprise Assistant

Phase 1 delivers a backend-first foundation for the DevX Enterprise Assistant with document registration, sample ingestion, filtering, lifecycle tracking, and a clean Spring Boot structure.

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

### Included Phase 1 foundation

- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL driver
- Flyway
- Actuator
- Lombok
- Validation

### Run locally

```bash
docker compose up -d postgres
cd backend
mvn spring-boot:run
```

Then open [http://localhost:8080/api/health](http://localhost:8080/api/health).

### Phase 1 API Surface

- `GET /api/health`
- `GET /api/documents?page=0&size=20`
- `GET /api/documents/{id}`
- `POST /api/documents`
- `PATCH /api/documents/{id}/status`
- `POST /api/ingestion/scan`

### Swagger UI

When the app is running, open:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

### Phase 1 Notes

- `sample-knowledge-base/` is a local demo source, not the final production storage strategy.
- Postgres currently stores document metadata and lifecycle information.
- `origin` distinguishes manual registrations from sample knowledge base discoveries.
- `status` tracks the document lifecycle: `DISCOVERED`, `INGESTED`, `FAILED`.
