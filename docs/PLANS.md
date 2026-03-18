# Plans Index

## Features (Exec Plans)

### Active Plans

| Plan | Feature | Status |
|------|---------|--------|
| [007-email-verification.md](exec-plans/active/007-email-verification.md) | F007 Email-Based Authentication with Email Verification | in_progress |

### Completed Plans

| Plan | Feature | Status |
|------|---------|--------|
| [001-library-search.md](exec-plans/completed/001-library-search.md) | F001 Anonymous Book Search | completed |
| [003-user-authentication.md](exec-plans/completed/003-user-authentication.md) | F003 User Authentication | completed |
| [004-prometheus-grafana-observability.md](exec-plans/completed/004-prometheus-grafana-observability.md) | F004 Prometheus & Grafana Observability | completed |
| [005-auth-gated-landing-page.md](exec-plans/completed/005-auth-gated-landing-page.md) | F005 Auth-Gated Landing Page | completed |
| [006-create-account-ux.md](exec-plans/completed/006-create-account-ux.md) | F006 Create Account Success UX | completed |

## Tasks (Task Exec Plans)

Tasks are small, focused changes. No design docs or features.json.

See **[TASKS.md](TASKS.md)** for the task index and workflow.

- **Task specs**: `docs/task-specs/`
- **Task exec plans**: `docs/task-exec-plans/active/` → `completed/`
- **Template**: `docs/templates/task-template.md`

## Tech Debt

See [exec-plans/tech-debt-tracker.md](exec-plans/tech-debt-tracker.md).

---

## How to Create a Feature Plan

1. Create `docs/exec-plans/active/{id}-{feature-name}.md`
2. Include: goal, acceptance criteria, implementation steps, test plan
3. Add to this index
4. Move to `exec-plans/completed/` when done, update features.json

## How to Create a Task

1. Use [task-template.md](templates/task-template.md) as a starting point
2. Create `docs/task-specs/TNNN-task-name.md`
3. See [TASKS.md](TASKS.md) for full workflow
