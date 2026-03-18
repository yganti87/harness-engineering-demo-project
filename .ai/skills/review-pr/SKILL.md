---
name: review-pr
description: Review a GitHub PR against its product spec and acceptance criteria. Use when the user says "review PR", "review PR #N", "check PR against spec", "does PR #N match the spec", or similar Phase 3 review triggers.
---

# Review PR

Fetches a pull request, locates the associated spec and exec plan, evaluates each
acceptance criterion against the PR diff, and writes a structured verdict to
`docs/pr-reviews/PR-{number}-review.md`.

## Step 1: Get the PR number

- If the user provided a number (e.g. "review PR #14"), use it.
- Otherwise run `gh pr list` and ask the user to identify the target PR.

## Step 2: Load credentials

```bash
[ -f .env ] && set -a && . ./.env && set +a
```

## Step 3: Fetch PR metadata and diff

```bash
gh pr view {number} --json title,body,headRefName,baseRefName
gh pr diff {number}
```

Extract:
- PR title and description body
- Branch name (used to infer feature/task ID)
- Any markdown links to spec or exec plan files in the PR body

## Step 4: Find the associated spec and exec plan

Use this lookup order — stop at the first match:

1. Explicit file links in the PR body (e.g. `[spec](docs/product-specs/...)`)
2. Feature or task ID parsed from the branch name (e.g. `feature/006-create-account-ux` → ID `006`)
3. Search `docs/product-specs/` for a file containing that ID or matching name
4. Search `docs/exec-plans/completed/` or `docs/exec-plans/active/` for the matching exec plan
5. If still ambiguous → ask the user to provide the spec file path

## Step 5: Read the spec and acceptance criteria

Read the spec file. Extract all numbered acceptance criteria (look for a section
titled "Acceptance Criteria" or similar). List each criterion for evaluation.

## Step 6: Analyze the PR diff against each criterion

For each acceptance criterion, evaluate the diff:
- Does the diff contain changes that address this criterion?
- Are there tests that verify the criterion's behavior?
- Are there any architectural violations (e.g. repository imported from controller, raw SQL in service layer)?
- Are there new code paths without corresponding tests?

## Step 7: Produce the review report

Build the report using this exact template:

```markdown
# PR Review: PR #{number} — {title}

**Branch**: {headRefName}
**Spec**: {spec-file-path}
**Exec Plan**: {exec-plan-file-path or "not found"}
**Reviewed**: {today's date}

## Acceptance Criteria

| # | Criterion | Status | Notes |
|---|-----------|--------|-------|
| 1 | {criterion text} | ✅ PASS | {evidence, or leave blank} |
| 2 | {criterion text} | ❌ FAIL | {what is missing} |
| 3 | {criterion text} | ⚠️ PARTIAL | {what was done vs. what is missing} |

## Code Quality

- **Architecture**: {any layer violations found, or "No violations detected"}
- **Tests**: {assessment of test coverage for new/changed code}
- **Style**: {checkstyle or naming observations visible in diff, or "No issues observed"}
- **Other**: {any additional observations — positive or negative}

## Verdict

**{APPROVED | CHANGES REQUESTED | NEEDS DISCUSSION}**

{1–2 sentence justification for the verdict.}
```

## Step 8: Write the report and print

```bash
mkdir -p docs/pr-reviews
```

Write the report to `docs/pr-reviews/PR-{number}-review.md`.
Print the full report to the console.
Inform the user: "Review complete. Report saved to `docs/pr-reviews/PR-{number}-review.md`."

## Verdict Rules

| Verdict | When to use |
|---------|-------------|
| **APPROVED** | All criteria ✅ PASS, no blocking code quality issues |
| **CHANGES REQUESTED** | Any criterion is ❌ FAIL, or a blocking ⚠️ PARTIAL exists |
| **NEEDS DISCUSSION** | Criteria are ambiguous, design concerns require human judgment, or the spec itself is unclear |

**Hard rules — never approve if:**
- Any layer dependency violation is present (e.g. repository imported from controller)
- New code paths exist with no corresponding tests
- A required Flyway migration was edited rather than added as a new version
