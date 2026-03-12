---
description: Run the full backend test suite (checkstyle, unit, architecture, integration) and summarize results
---

Run the complete test suite for the library backend and report results.

## Steps

1. Check that Docker is running (required for Testcontainers integration tests)
2. Run `./scripts/run-tests.sh` from the project root
3. Summarize results:
   - Total tests run
   - Any failures with test name, expected vs actual
   - Any Checkstyle violations with file name and line number
   - Any ArchUnit violations with offending class and remediation instructions
4. On success: "All tests passed. Safe to commit."
5. On failure: List each failure and suggest the fix from docs/COMMON_PITFALLS.md

## Quick Commands

```bash
# Full suite
./scripts/run-tests.sh

# Unit tests only (faster, no Docker)
cd backend && mvn test -Dgroups='!integration' -q

# Integration tests only
cd backend && mvn test -Dgroups=integration -q

# Architecture tests only
cd backend && mvn test -Dtest='*LayerDependency*' -q

# Checkstyle only
cd backend && mvn checkstyle:check

# Frontend tests
cd frontend && python -m pytest tests/ -v
```

## Interpreting Failures

- **Checkstyle**: Check file and line. See docs/CODING_STYLE.md for the rule. Error message includes the fix.
- **ArchUnit violation**: A class imports from a forbidden layer. Error message says which class and where it should be moved. See docs/ARCHITECTURE.md.
- **Testcontainers "no Docker"**: Start Docker Desktop, then retry.
- **Integration test assertion**: Seed data may have changed. Check V1__create_library_schema.sql matches test expectations.
- **Compilation error**: Check the error message for missing imports. Verify layer order (types → config → repository → service → controller).
