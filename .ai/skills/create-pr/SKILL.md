---
name: create-pr
description: Creates well-formatted GitHub PR content from feature or task implementation. Use when the user asks to create a PR, draft a pull request, or prepare PR content for completed work.
---

# Create GitHub PR

## Instructions

When creating PR content from completed implementation:

1. **Analyze changes**: Review the git diff or implementation summary to understand what was done
2. **Identify scope**: Determine if it's feat, fix, chore, refactor, docs, or test
3. **Generate title** using Conventional Commits: `type(scope): brief description`
4. **Generate description** using the template below

## PR Title Format

```
<type>(<scope>): <subject>

Types: feat | fix | chore | refactor | docs | test
Scope: optional, e.g. backend, frontend, search, auth
```

Examples:
- `feat(search): add book search by title and author`
- `fix(api): correct 404 response format`
- `chore(deps): upgrade Spring Boot to 3.2.2`

## PR Description Template

```markdown
## Summary
[1-2 sentence overview of what this PR does]

## Changes
- [List key changes, one per line]
- [Reference specific components if relevant]

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual verification steps if applicable

## Notes
[Optional: migration steps, breaking changes, follow-up work]
```

## Output

Provide:
1. **Title** — copy-paste ready
2. **Body** — filled template, copy-paste ready
3. **Optional**: `gh pr create` command with `--title` and `--body` (escape newlines for shell)

## Example

Input: User completed book search feature implementation.

Output:

**Title:** `feat(search): add book search by title and author`

**Body:**
```markdown
## Summary
Adds search endpoint and frontend UI to search books by title and author.

## Changes
- `BookController`: GET /api/books/search with query params
- `BookService`: search by title/author using repository
- `BookRepository`: findByTitleContainingIgnoreCase, findByAuthorContainingIgnoreCase
- Streamlit: search form and results display

## Testing
- [x] Unit tests for BookService.searchBooks
- [x] Integration test for search endpoint
- [x] Manual check via frontend

## Notes
None.
```

**Create via CLI:**
```bash
gh pr create --title "feat(search): add book search by title and author" --body "## Summary
Adds search endpoint..."
```
