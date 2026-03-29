# Onboarding Guide

## Development Setup

- Install Java 21
- Start PostgreSQL with Docker Compose
- Run the backend with Maven
- Open Swagger UI to inspect the REST endpoints
- Verify Flyway migrations were applied before testing ingestion

## Team Practices

- Keep changes small and reviewable
- Prefer database migrations over manual schema edits
- Add logs around startup and ingestion paths
- Store document metadata in PostgreSQL and keep retrieval chunks traceable to a source document
- Use the retrieval endpoint before wiring any AI orchestration so search behavior is observable

## Local Verification Checklist

During local setup, verify that the health endpoint returns Phase 2, the ingestion scan registers sample documents, and the retrieval search can find terms like PostgreSQL, Flyway, and authentication. If search results look too narrow, add richer sample documents rather than changing retrieval logic first. That keeps debugging focused on the quality of the knowledge base rather than the query code.
