# Security and Authentication Guide

## Authentication Baseline

All non-public endpoints should be protected by Spring Security. Public endpoints such as health checks, Swagger documentation, and approved retrieval test routes may be temporarily open during early phases, but production-facing business APIs must require authentication. The project should evolve toward token-based authentication instead of relying on temporary development defaults.

## Authorization Principles

Authorization should be explicit and policy driven. Teams should define who can register documents, who can trigger ingestion, and who can query sensitive retrieval results. Document origin and audit metadata become important once internal runbooks, onboarding material, and authentication guidelines are searchable through a single assistant.

## Secret Handling

Credentials for PostgreSQL, external APIs, and future model providers must come from environment variables or a managed secret store. Secrets must never be committed into the repository. Local `.env` usage is acceptable for development, but committed examples should stay in `.env.example` only.

## Security Review Expectations

Before enabling a chat endpoint backed by retrieval, review the exposure of source paths, chunk content, and internal operational details. Retrieval can accidentally surface sensitive information if the sample knowledge base later grows into real enterprise documentation. Logging should help trace access without storing secret material in application logs.
