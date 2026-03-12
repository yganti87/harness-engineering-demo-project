# Task Index

> Tasks are small, focused changes. No design docs or features.json updates.
> See [task template](templates/task-template.md) for the spec format.

## Active Tasks

| ID | Task | Scope | Status |
|----|------|-------|--------|
| — | — | — | — |

## Completed Tasks

| ID | Task | Completed |
|----|------|-----------|
| T001 | [Bring types/dto quality score to A](task-specs/T001-types-dto-quality-a.md) | 2026-03-12 |

---

## How to Create a Task

1. Copy [docs/templates/task-template.md](templates/task-template.md)
2. Save as `docs/task-specs/TNNN-task-name.md` (e.g. T001-add-isbn-validation.md)
3. Fill in description, acceptance criteria, and verification steps
4. Create exec plan in `docs/task-exec-plans/active/` when ready to implement
5. Add entry to this index
6. Move spec to `docs/task-exec-plans/completed/` when done

## Task vs Feature

| Aspect | Task | Feature |
|--------|------|---------|
| Size | Small, 1–2 hours | Large, multi-session |
| Design doc | No | Yes (product-specs) |
| features.json | No | Yes |
| Exec plan | task-exec-plans | exec-plans |
| Tests | Targeted subset | Full suite |
