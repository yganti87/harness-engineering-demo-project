# Task Template

> Use this template when drafting a task description. Copy to `docs/task-specs/` as `TNNN-task-name.md`.
> Tasks are small, focused changes — no design docs, no features.json updates.

## Task: [Short descriptive title]

**Task ID**: T001  
**Status**: draft | in_progress | completed  
**Scope**: backend | frontend | both | infra | docs

---

## Context

_Brief background: why is this task needed? (1–3 sentences.)_

---

## Description

_What should be done? Be specific._

- [ ] Step 1
- [ ] Step 2
- [ ] Step 3

---

## Acceptance Criteria

_Testable conditions. The task is done when all are true._

1. Criterion 1
2. Criterion 2
3. Criterion 3

---

## Verification

_Which tests or checks must pass to confirm the task is correct?_

| Check | Command / Action |
|-------|------------------|
| Checkstyle | `cd backend && mvn checkstyle:check` |
| Unit tests | `cd backend && mvn test -Dgroups='!integration'` or `-Dtest=SpecificTest` |
| Integration tests | `cd backend && mvn test -Dgroups=integration` (if applicable) |
| Arch tests | `cd backend && mvn test -Dtest='*LayerDependency*'` (if new classes) |
| Manual | _e.g., curl endpoint, open UI, verify behavior_ |

---

## Out of Scope

_What this task does NOT include._

---

## Files Touched (Expected)

_Optional: list files likely to be created or modified._
