# Design Doc 001: Initial Architecture

**Date**: 2026-03-12
**Status**: Accepted
**Author**: Harness Setup

## Problem

We need a library catalog application that:
1. Can be developed entirely by coding agents
2. Is maintainable and extensible without manual intervention
3. Provides strong feedback loops (tests, linting, observability)

## Decision

### Technology Choices

| Component | Choice | Rationale |
|-----------|--------|-----------|
| Backend | Java 17 + Spring Boot 3.2.x | Mature ecosystem, strong typing, excellent Spring Data JPA |
| Frontend | Streamlit | Rapid iteration, minimal boilerplate, Python ecosystem |
| Database | PostgreSQL 15 | Full-featured, excellent JSONB + full-text search for future features |
| ORM | Spring Data JPA + Hibernate | Convention over configuration, powerful query derivation |
| Migrations | Flyway | Version-controlled schema, checksums prevent drift |
| API Docs | SpringDoc OpenAPI | Auto-generated from code annotations |
| Architecture enforcement | ArchUnit | Compile-time-equivalent layer rule enforcement |
| Code style | Checkstyle (Google style) | Deterministic, CI-enforceable |
| Containerization | Docker Compose | Simple local dev, mirrors production topology |

### Layer Model

Strict unidirectional: `types → config → repository → service → controller`

This was chosen because:
- Agents are most productive in environments with predictable structure
- Violations are caught automatically (ArchUnit) rather than in code review
- Each layer has a single responsibility, making test isolation easy

### API Design

REST with standard envelope. Every response is `ApiResponse<T>` so agents never need to decide the response shape.

### Logging Strategy

Structured JSON logs to file, mounted to host. Agents can access logs without exec-ing into containers.
Actuator `/logfile` endpoint provides HTTP access without file system access.

## Alternatives Considered

| Alternative | Rejected Because |
|-------------|-----------------|
| Node.js backend | Less type safety, harder to enforce layer constraints |
| Flask/FastAPI | Less structure by default, requires more harness setup |
| MongoDB | Schema flexibility works against agent predictability |
| Liquibase | Similar to Flyway, Flyway preferred for simplicity |
