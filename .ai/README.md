# Library App — AI Agent Guide

> This file extends [AGENTS.md](../AGENTS.md) with coding-assistant instructions.
> **Always read AGENTS.md first.**

Works with any AI coding agent (Claude, Cursor, etc.). This folder is the single source of truth for agent config. Cursor and Claude reference it via `.cursor/rules/` and `.claude` symlink.

## Agent Skills (Slash Commands)

### Project Skills (.ai/skills/)

| Skill | Use when |
|-------|----------|
| [create-pr](skills/create-pr/SKILL.md) | User asks to create a GitHub PR, draft a pull request, or prepare PR content for completed implementation |
| [unit-test-runner](skills/unit-test-runner/SKILL.md) | Running unit tests for plan execution; analyzing Maven Surefire output to determine success/failure |
| [integration-test-runner](skills/integration-test-runner/SKILL.md) | Running integration tests for plan execution; analyzing Maven Failsafe output (do not trust exit code alone) |

### Slash Commands (commands/)

The following slash commands are available:

| Command | Description |
|---------|-------------|
| `/run-tests` | Run full test suite (checkstyle + unit + integration + arch) |
| **task-spec** | Create task spec from description (use task-spec agent) |
| **task-exec** | Implement task and run verification checks (use task-exec agent) |
| `/logs [service] [lines]` | Show recent logs (backend/frontend/db) |
| `/build` | Build all Docker images |
| `/migrate` | Apply pending Flyway migrations |
| `/db-shell` | Open interactive psql session |

## Before Any Task or Feature

0. **Sync with main**: `git fetch origin main && git merge origin/main` — resolve merge conflicts before proceeding. Do not start implementation until the branch is up to date.

## Workflow for Implementing a Feature

1. Read `features.json` to understand the current feature's acceptance criteria
2. Read `docs/ARCHITECTURE.md` — understand the layer constraints before writing any code
3. Read `docs/PATTERNS.md` — follow existing patterns exactly
4. Read `docs/COMMON_PITFALLS.md` — avoid known failure modes
5. Implement in layer order: types → config → repository → service → controller
6. Write unit tests alongside implementation
7. Write integration tests in `src/test/.../integration/`
8. Run `/run-tests` to verify
9. Check `/logs` if any tests fail for diagnostic context
10. Update `features.json` status when feature is complete

## Workflow for Tasks (Small Changes)

Tasks are small, focused changes — no design docs, no features.json.

0. **Sync with main first** (see above). Then:
1. **Draft task spec**: Use `task-spec` agent or [docs/templates/task-template.md](../docs/templates/task-template.md)
2. **Create spec**: Save to `docs/task-specs/TNNN-task-name.md`
3. **Implement**: Use `task-exec` agent or implement manually from the spec
4. **Verify**: Run the checks listed in the task spec's Verification section (subset of full suite)
5. **Done**: Move exec plan to `docs/task-exec-plans/completed/`, update [docs/TASKS.md](../docs/TASKS.md)

## Debugging Failed Tests

1. Run `/run-tests` and capture the failure message
2. For Checkstyle: fix the violation per `docs/CODING_STYLE.md`
3. For ArchUnit: the error message contains the offending class and fix instructions
4. For integration test failures: run `/logs backend 100` to see application logs
5. For Testcontainers failures: verify Docker is running

## Environment Setup

Add `GH_TOKEN=ghp_xxx` to `.env` if AI agents should run `gh pr create` (automation needs it; interactive `gh auth login` does not). See `.env.example`.

```bash
# Check Docker is running
docker info

# Check .env exists (required for docker-compose)
ls -la .env || cp .env.example .env

# Start services
./scripts/start.sh

# Verify health
curl http://localhost:8080/actuator/health
```

## File Editing Rules

- **Never edit**: existing Flyway migrations (`V1__*.sql`, `V2__*.sql`, etc.)
- **Always add**: new migrations as `V{n+1}__description.sql`
- **Never add**: direct SQL in service or controller classes
- **Always update**: `features.json` when feature status changes
- **Update AGENTS.md**: when you discover a new constraint or fix a recurring agent failure

## Common Commands Reference

```bash
# Local backend development (without Docker)
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run specific test class
cd backend && mvn test -Dtest=BookServiceTest

# Tail backend logs live
docker compose logs backend --follow

# Check DB schema
docker compose exec db psql -U library_user -d library_db -c "\dt"

# Rebuild only backend image (after code changes)
docker compose build backend && docker compose up -d backend
```
