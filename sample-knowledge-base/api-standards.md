# API Standards

## REST Conventions

Our backend APIs should use resource-oriented paths, explicit HTTP verbs, and predictable response envelopes where needed. Health checks and simple read endpoints may return lightweight responses, but business APIs should return stable JSON structures that are easy for clients to parse and test. Pagination parameters such as page and size should be validated and documented.

## Error Handling

Every public API should return consistent error payloads. Validation failures must explain which field or request parameter is invalid. Conflict errors should clearly state why a resource cannot be created or updated. Unexpected server failures should not leak internal stack traces to clients, but they should be logged on the server with enough context to troubleshoot the issue.

## Search and Retrieval

Retrieval endpoints should expose query parameters that are easy to reason about. Keyword search is acceptable as an intermediate step, but retrieval services should eventually rank context using richer signals than plain substring matching. Search responses should include document identifiers, source paths, and chunk references so downstream chat responses can provide citations.

## Observability

Controllers should log major actions such as ingestion scans, retrieval searches, and lifecycle status changes. Logs should include query text when it is safe, along with identifiers like document id or source path. Structured observability makes it easier to debug PostgreSQL-backed search behavior and to compare raw chunk search with future semantic retrieval.
