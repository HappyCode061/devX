# Incident Response Runbook

## Detection and Triage

When the service becomes unavailable, start with the health endpoint, recent application logs, and PostgreSQL connectivity. Determine whether the issue is startup failure, ingestion failure, migration failure, or retrieval degradation. Capture the exact error message before restarting anything so the team can distinguish transient issues from reproducible defects.

## Database Checks

If ingestion or retrieval stops working, confirm that PostgreSQL is running, the configured database URL is correct, and Flyway migrations are up to date. A mismatch between entity definitions and schema migrations can break startup or cause runtime persistence errors. Always verify whether new tables such as document chunks were created successfully after a deployment.

## Retrieval Diagnostics

If chunk search returns no results, first confirm that documents were ingested and chunk generation actually occurred. Then test a known keyword from the sample knowledge base, such as PostgreSQL, authentication, or retrieval. If chunks exist but matching still fails, inspect the stored chunk content and the query normalization logic before changing the ranking strategy.

## Recovery Steps

Preferred recovery starts with evidence collection, followed by the smallest safe corrective action. Restart the Spring Boot application only after checking logs. Rerun ingestion only after verifying that duplicate protections behave as expected. If a schema problem is suspected, inspect Flyway history instead of editing the database manually.
