# Harness Engineering Demo Project

A **spec-driven, agent-built** library catalog application. The goal of this project is to learn how to build an effective harness for coding agents — one where every feature is specified, planned, implemented, tested, and verified by AI agents with minimal human intervention.

No application code in this repository was written by hand. Every line was produced by Claude Code agents following structured product specs and execution plans.

## The Approach: Spec-Driven Development

The workflow follows a repeatable cycle:

1. **Product Spec** — Define what to build (user story, API contract, acceptance criteria)
2. **Execution Plan** — Break it into phased implementation steps with a test plan
3. **Agent Implementation** — A spec-exec agent implements the plan layer by layer
4. **Test-Fix Loop** — The agent runs tests, diagnoses failures, and fixes until green
5. **Verification** — API checks, screenshots, and video evidence captured automatically
6. **PR & Review** — A create-pr skill packages everything for review

Failures become constraints. Every time an agent makes a mistake, it gets documented in `docs/COMMON_PITFALLS.md` and enforced going forward. The harness improves with every feature.

## The Application

A **library catalog app** where users can search books, register with email, verify their account, and browse the catalog. The stack:

| Component | Technology | Port |
|-----------|-----------|------|
| Backend | Java 17 + Spring Boot 3.2.x | 8080 |
| Frontend | Python 3.11 + Streamlit | 8501 |
| Database | PostgreSQL 15 | 5433 |
| Monitoring | Prometheus + Grafana | 9090 / 3000 |
| Email (dev) | Mailpit | 8025 |

## Quick Start

```bash
# Start all services
./scripts/start.sh

# Run all tests
./scripts/run-tests.sh

# Stop all services
./scripts/stop.sh
```

Open the app at http://localhost:8501, Swagger at http://localhost:8080/swagger-ui.html, and Mailpit at http://localhost:8025.

## Project Structure

### Core Application

| Directory | Description |
|-----------|-------------|
| `backend/` | Spring Boot REST API — controllers, services, repositories, DTOs |
| `frontend/` | Streamlit UI — single `app.py` with search, auth, and catalog views |
| `database/` | Database configuration and seed data |
| `docker-compose.yml` | All services orchestrated together |

### Agent Harness

| Directory | Description |
|-----------|-------------|
| `AGENTS.md` | Top-level agent guide — architecture, constraints, quick start |
| `.ai/agents/` | Agent definitions: `feature-spec`, `spec-exec`, `task-spec`, `task-exec` |
| `.ai/skills/` | Reusable skills: `run-tests`, `create-pr`, `exec-plan`, `review-pr`, test runners |
| `features.json` | Feature registry with acceptance criteria and implementation status |

### Specs and Plans

| Directory | Description |
|-----------|-------------|
| `docs/product-specs/` | Product specifications for each feature |
| `docs/exec-plans/` | Execution plans (active and completed) with phased steps |
| `docs/task-specs/` | Smaller task specifications |
| `docs/task-exec-plans/` | Execution plans for tasks |
| `docs/verification-output/` | Screenshots, videos, and test evidence for completed features |

### Documentation (Agent-Enforced)

| File | Description |
|------|-------------|
| `docs/ARCHITECTURE.md` | Layer dependency rules enforced by ArchUnit |
| `docs/PATTERNS.md` | Code patterns with copy-paste examples |
| `docs/COMMON_PITFALLS.md` | Known agent failure modes — turns mistakes into guardrails |
| `docs/CODING_STYLE.md` | Style rules enforced by Checkstyle |
| `docs/TESTING.md` | Test strategy: unit, integration, architecture |
| `docs/SECURITY.md` | Security constraints for auth, JWT, input validation |

### Scripts

| Script | Description |
|--------|-------------|
| `scripts/start.sh` | Start all Docker services with health checks |
| `scripts/stop.sh` | Stop all services |
| `scripts/run-tests.sh` | Full test suite: checkstyle, unit, architecture, integration |
| `scripts/build.sh` | Build Docker images |
| `scripts/logs.sh` | View service logs |
| `scripts/capture.sh` | Take screenshots and generate verification videos |

## Features Built

| ID | Feature | Status |
|----|---------|--------|
| F001 | Anonymous Book Search | Completed |
| F002 | Book Detail View | Completed |
| F003 | Structured Logging | Completed |
| F004 | Prometheus Metrics | Completed |
| F005 | User Authentication (JWT) | Completed |
| F006 | Authenticated Book Reviews | Completed |
| F007 | Email Verification | Completed |

## Architecture

The backend enforces a strict layer architecture via ArchUnit:

```
types → config → repository → service → controller
```

- Controllers never import repositories directly
- All responses use a standard `ApiResponse<T>` envelope
- Flyway manages database migrations (never edit existing migrations)
- Integration tests use Testcontainers (real PostgreSQL, no mocks)

## License

This project is for learning and demonstration purposes.
