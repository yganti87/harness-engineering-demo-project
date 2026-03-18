# Development Workflow

This project uses a 3-phase development loop. Phases 1 and 3 always run locally.
Phase 2 can run locally or inside a GitHub Codespace.

## Overview

| Phase | Name | Runs on | Purpose |
|-------|------|---------|---------|
| 1 | Spec & Plan | Local | Write product spec, generate and review exec plan |
| 2 | Execution | Local **or** Codespace | Implement code, run tests, commit, push |
| 3 | PR Review | Local | Review resulting PR against spec acceptance criteria |

---

## Phase 1: Spec & Plan (always local)

### Purpose

Interactively define what to build and how. The output is a committed exec plan file
in `docs/exec-plans/active/` or `docs/task-exec-plans/`.

### Steps

1. Identify or create the feature entry in `features.json`
2. Draft the product spec using the `feature-spec` agent or write manually in `docs/product-specs/`
3. Generate the execution plan — save to `docs/exec-plans/active/{id}-{name}.md` (features) or `docs/task-exec-plans/{id}-{name}.md` (tasks)
4. Review acceptance criteria and confirm the plan before moving to Phase 2

### Relevant Files

| File / Directory | Purpose |
|-----------------|---------|
| `features.json` | Feature registry — IDs, status, acceptance criteria |
| `docs/product-specs/` | Product spec files |
| `docs/exec-plans/active/` | Active feature exec plans |
| `docs/task-exec-plans/` | Task exec plans |
| `.ai/agents/feature-spec.md` | Agent for drafting feature specs |
| `.ai/agents/task-spec.md` | Agent for drafting task specs |

---

## Phase 2: Execution

Use the `/exec-plan` skill (say "execute the plan for feature X"). The skill identifies
the plan file and asks whether to run locally or in a Codespace before doing anything.

### Option A — Local (default)

**When to use:**
- You have Claude Pro or Max — local execution uses **zero Anthropic API tokens** (Claude runs natively through the UI)
- You want to interact with the agent during execution or monitor it in real time
- The task is short enough to run without interruption

**How it works:**

The `exec-plan` skill delegates to the `spec-exec` agent (features) or `task-exec` agent (tasks).
The agent reads the plan, asks for your confirmation, then implements code in layer order,
runs the test–fix loop, and commits when all tests pass.

**Token cost:** None — uses your Claude Pro/Max subscription.

### Option B — Codespace

**When to use:**
- You want to free up your local machine while a long implementation runs remotely
- The feature requires all Docker services to be running (they auto-start in the Codespace)
- You are on a pay-per-token API plan and want to offload compute to the Codespace machine

**How it works:**

The `exec-plan` skill builds a prompt string and passes it to `./scripts/codespace-exec.sh`,
which SSHes into your running Codespace and runs `claude --print --dangerously-skip-permissions`
headlessly. Execution output streams back to your local terminal.

**Token cost:** Consumes `ANTHROPIC_API_KEY` tokens (set as a Codespace secret).

**Prerequisites:** See [Configuration](#configuration) below.

### Decision Guide

| Factor | Local | Codespace |
|--------|-------|-----------|
| Claude Pro/Max plan | ✅ Preferred — zero API cost | ❌ Uses API tokens |
| Long-running task | ⚠️ Risk of local interruption | ✅ Runs safely on remote machine |
| Needs all Docker services | Requires local Docker running | ✅ Auto-started in Codespace |
| Real-time monitoring | ✅ Terminal output | ✅ Streamed via SSH |
| Interactive approval gate | ✅ Built into spec-exec agent | ⚠️ Headless — pre-approved prompt |

---

## Phase 3: PR Review (always local)

### Purpose

Verify that the resulting PR satisfies every acceptance criterion from the spec.
Produces a structured, reviewable report.

### Steps

1. Confirm the PR number from `gh pr list` or the push output
2. Use the `/review-pr` skill — say "review PR #N" or "check PR #N against the spec"
3. The skill fetches the PR diff, finds the spec, and produces a report at `docs/pr-reviews/PR-{N}-review.md`
4. Review the report: approve, request changes, or open a discussion

### Output Format

```
## Acceptance Criteria
| # | Criterion | Status | Notes |
|---|-----------|--------|-------|
| 1 | ... | ✅ PASS | ... |
| 2 | ... | ❌ FAIL | what is missing |

## Code Quality
- Architecture: ...
- Tests: ...

## Verdict
APPROVED | CHANGES REQUESTED | NEEDS DISCUSSION
```

---

## Configuration

### `.codespace.env` (for Phase 2 Codespace path)

Create this file in the project root — it is gitignored:

```bash
CODESPACE_NAME=<your-codespace-name>
```

Find your codespace name:

```bash
gh codespace list
```

### `.env` Required Fields

```bash
GH_TOKEN=ghp_xxx              # GitHub PAT — used by gh CLI and automation
ANTHROPIC_API_KEY=sk-ant-xxx  # Only required for Codespace execution
```

### Prerequisites

| Requirement | How to check |
|-------------|-------------|
| `gh` CLI authenticated | `gh auth status` |
| Codespace created and running | `gh codespace list` |
| `ANTHROPIC_API_KEY` set as Codespace secret | GitHub → Settings → Codespaces → Secrets |
| Docker running (local execution) | `docker info` |

---

## Quick Reference

```bash
# ── Phase 1 ──────────────────────────────────────────────────────────────
# Check feature status
cat features.json | grep -A5 '"status"'

# List active exec plans
ls docs/exec-plans/active/

# ── Phase 2 ──────────────────────────────────────────────────────────────
# Check codespace status before remote execution
./scripts/codespace-exec.sh --status

# SSH into codespace interactively (run claude manually)
./scripts/codespace-exec.sh --interactive

# Run Claude headlessly in codespace with a prompt
./scripts/codespace-exec.sh "Execute the plan at docs/exec-plans/active/007-my-feature.md"

# ── Phase 3 ──────────────────────────────────────────────────────────────
# List open PRs
gh pr list

# View a specific PR
gh pr view 14

# Read a generated review report
cat docs/pr-reviews/PR-14-review.md
```
