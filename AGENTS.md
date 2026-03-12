# Library App — Agent Guide

> **This file is the table of contents.** Read it fully first, then follow links to deeper docs.
> Update this file whenever an agent fails a task — turn failures into constraints.

## Quick Start

```bash
# Start all services (DB + backend + frontend)
./scripts/start.sh

# Run all tests (requires Docker for integration tests)
./scripts/run-tests.sh

# View backend logs
./scripts/logs.sh backend 50

# Build images only (no start)
./scripts/build.sh

# Stop all services
./scripts/stop.sh
```

## Project Overview

**Library catalog app** — users search books without authentication.

| Component | Technology | Port |
|-----------|-----------|------|
| Database | PostgreSQL 15 | 5433 (host), 5432 (container) |
| Backend | Java 17 + Spring Boot 3.2.x | 8080 |
| Frontend | Python 3.11 + Streamlit | 8501 |

- **API docs**: http://localhost:8080/swagger-ui.html
- **Health check**: http://localhost:8080/actuator/health
- **Logs via HTTP**: http://localhost:8080/actuator/logfile

## Architecture — CRITICAL

Dependencies flow in ONE direction only:

```
types → config → repository → service → controller
```

**Enforced by `LayerDependencyTest.java` (ArchUnit)** — violations fail the build.

- `com.library.types`: DTOs, enums. No dependencies on other layers.
- `com.library.config`: App configuration. May import `types` only.
- `com.library.repository`: JPA entities + repositories. May import `types`, `config`.
- `com.library.service`: Business logic. May import `types`, `config`, `repository`.
- `com.library.controller`: REST endpoints + exception handler. May import `types`, `service`.
- **NEVER** import `repository` from `controller`. Always go through `service`.
- **NEVER** import `controller` from `service` or below.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for full details.

## API Response Envelope

**Every** controller method returns `ApiResponse<T>`. Never return raw DTOs.

```json
{ "status": 200, "data": { ... }, "error": null, "timestamp": "2026-03-12T10:00:00Z" }
{ "status": 404, "data": null, "error": "Book not found: id=abc", "timestamp": "..." }
```

## Testing

```bash
# Unit tests only (no Docker needed)
cd backend && mvn test -Dgroups='!integration'

# Integration tests only (requires Docker)
cd backend && mvn test -Dgroups=integration

# Architecture tests
cd backend && mvn test -Dtest='*LayerDependency*'

# Checkstyle
cd backend && mvn checkstyle:check
```

See [docs/TESTING.md](docs/TESTING.md) for conventions.

## Logging

Backend logs are structured JSON at:
- **File**: `./logs/backend/app.log`
- **HTTP**: `curl http://localhost:8080/actuator/logfile`
- **Docker**: `docker compose logs backend --follow`

See [docs/RELIABILITY.md](docs/RELIABILITY.md) for logging conventions.

## Key Constraints (do not violate)

1. All API responses use `ApiResponse<T>` envelope — see `types/dto/ApiResponse.java`
2. Layer dependency order must be respected — ArchUnit enforces this
3. Never modify existing Flyway migrations — create new `V{n}__description.sql` files
4. All field validation uses `jakarta.validation` annotations on DTOs
5. No raw SQL in service or controller layers — use Spring Data JPA in repository only
6. Checkstyle runs at `validate` phase — fix violations before pushing

## Documentation Map

| File | Contents |
|------|---------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layer rules, enforcement, dependency diagram |
| [docs/CODING_STYLE.md](docs/CODING_STYLE.md) | Java + Python style guide |
| [docs/PATTERNS.md](docs/PATTERNS.md) | Repository/service/DTO/error patterns with examples |
| [docs/TESTING.md](docs/TESTING.md) | Unit/integration/arch test conventions |
| [docs/API_REFERENCE.md](docs/API_REFERENCE.md) | All REST endpoints with curl examples |
| [docs/COMMON_PITFALLS.md](docs/COMMON_PITFALLS.md) | **Read this before writing any code** |
| [docs/SECURITY.md](docs/SECURITY.md) | Input validation, CORS, SQL injection prevention |
| [docs/RELIABILITY.md](docs/RELIABILITY.md) | Error handling, logging, health checks |
| [docs/FRONTEND.md](docs/FRONTEND.md) | Streamlit conventions |
| [docs/PRODUCT_SENSE.md](docs/PRODUCT_SENSE.md) | Product vision + feature roadmap |
| [features.json](features.json) | Feature status tracking |
| [backend/AGENTS.md](backend/AGENTS.md) | Backend-specific agent instructions |
| [frontend/AGENTS.md](frontend/AGENTS.md) | Frontend-specific agent instructions |
| [.ai/README.md](.ai/README.md) | Agent skills, workflows, slash commands (single source of truth) |

## Skills Available

| Command | Purpose |
|---------|---------|
| `/run-tests` | Run full test suite and summarize results |
| `/logs` | Show recent logs for a service |
| `/build` | Build Docker images |
| `/migrate` | Run Flyway DB migrations |
| `/db-shell` | Open psql shell |
