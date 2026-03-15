---
name: create-pr
description: Creates well-formatted GitHub PR from feature or task implementation. Drafts commit, commits to feature branch, pushes, and opens PR via gh. Use when the user asks to create a PR, draft a pull request, or prepare PR content for completed work.
---

# Create GitHub PR

## Workflow

Execute this full flow: draft commit → commit → push → open PR.

### Step 1: Analyze changes

- Run `git status` and `git diff --stat`
- Review changes to understand scope and type (feat, fix, chore, refactor, docs, test)

### Step 2: Ensure feature branch

- If on `main` or `master`: create and switch to `git checkout -b <type>/<scope>-<slug>` (e.g. `feat/search-add-book-search`)
- If already on a feature branch: stay on it

### Step 3: Ensure execution plan and test artifacts are checked in

- **IMPORTANT**: Before creating the PR, verify that the execution plan has been committed.
- If this is a feature (F00x), check that `docs/exec-plans/completed/{id}-{name}.md` exists. If not, create it from the plan file.
- Update `docs/PLANS.md` to move the feature from Active to Completed (or add it to Completed if missing).
- Include test screenshots in `test-output/F00x/` if browser verification was performed.
- Commit any missing execution plan or test artifacts before proceeding.

### Step 4: Draft and create commit

- Stage relevant files: `git add <paths>` (exclude unrelated files like `.env`, IDE config)
- Generate commit message: `type(scope): brief description` (Conventional Commits)
- Run `git commit -m "<message>"`

### Step 5: Push

- Run `git push -u origin <branch>`

### Step 6: Open PR

- Load GH_TOKEN from project `.env` if present (needed for non-interactive use): `[ -f .env ] && set -a && . ./.env && set +a`
- Run `gh pr create --title "<commit title>" --body "<description>"` (see body template below)

## Formats

**Commit/PR title:** `type(scope): subject` — types: feat, fix, chore, refactor, docs, test

**PR body template:**
```markdown
## Summary
[1-2 sentence overview]

## Changes
- [Key changes, one per line]

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual verification if applicable

## Notes
[Optional]
```

## Shell note

For `gh pr create --body`, pass body as a single string. Use `\n` for newlines or a here-doc. Avoid unescaped quotes in the body.
