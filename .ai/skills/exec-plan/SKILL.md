---
name: exec-plan
description: Execute an exec plan or product spec — implement code, run tests, commit. Use when the user says "execute the plan", "implement the spec", "run the exec plan for feature X", "implement feature X", or any similar execution trigger.
---

# Execute Plan

Routes plan execution to the local `spec-exec`/`task-exec` agent or to a remote
GitHub Codespace via `scripts/codespace-exec.sh`. Always asks before running.

## Step 1: Identify the exec plan file

- **Explicit path in prompt** → use it directly
- **Feature ID or name** (e.g. "feature 007" or "book-search") → search `docs/exec-plans/active/` for a filename containing the ID or name
- **Task ID or name** → search `docs/task-exec-plans/` for a match
- **Ambiguous** → run `ls docs/exec-plans/active/ docs/task-exec-plans/ 2>/dev/null` and ask the user to confirm which plan to use

## Step 2: Determine plan type

- File in `docs/exec-plans/active/` → **feature plan** → delegate to `spec-exec` agent
- File in `docs/task-exec-plans/` → **task plan** → delegate to `task-exec` agent

## Step 3: Ask the user where to run

Before doing anything else, ask:

> Found exec plan: `{file-path}`
>
> Run locally or in a GitHub Codespace?
> - **local** *(default)* — runs in this Claude session. If you have Claude Pro or Max, this uses zero Anthropic API tokens.
> - **codespace** — SSHes into your Codespace and runs Claude headlessly. Requires `.codespace.env` to be configured. Consumes API tokens.
>
> Reply `local`, `codespace`, or press Enter for local:

## Step 4a: Local execution (default)

If the user replies `local`, presses Enter, or gives no clear answer:

- For **feature plans**: invoke the `spec-exec` agent with:
  > "Execute the exec plan at {file-path}. Read the plan, confirm it with me, then implement it following the spec-exec workflow."
- For **task plans**: invoke the `task-exec` agent with the same phrasing.
- The agent handles the approval gate and the test–fix loop. Do not bypass its confirmation step.

## Step 5b: Codespace execution

If the user replies `codespace`:

1. Check `.codespace.env` exists and contains `CODESPACE_NAME`:
   ```bash
   [ -f .codespace.env ] && source .codespace.env
   ```
   If `CODESPACE_NAME` is empty or the file is missing, print:
   > `.codespace.env` not configured. Create it with `CODESPACE_NAME=<name>`.
   > Find your codespace name: `gh codespace list`
   > See `docs/DEVELOPMENT_WORKFLOW.md` → Configuration for full setup instructions.
   Then stop.

2. Build the execution prompt (use double quotes — no unescaped single quotes in the path):
   ```
   Read the exec plan at {file-path} and implement it. Follow the workflow in
   .ai/agents/spec-exec.md (or task-exec.md for task plans). Run the test-fix
   loop until all tests pass. Commit and push when done.
   ```

3. Run the codespace script:
   ```bash
   ./scripts/codespace-exec.sh "{prompt}"
   ```

4. Inform the user: output will stream from the Codespace. They can also SSH in
   interactively with `./scripts/codespace-exec.sh --interactive` to monitor progress.

## Notes

- Never invoke `spec-exec` if the user has not approved the plan — the agent will ask; do not skip that gate.
- Task exec plans live in `docs/task-exec-plans/` and use the `task-exec` agent, not `spec-exec`.
- If `.codespace.env` is missing, always give setup instructions rather than failing silently.
- Prompt strings passed to `codespace-exec.sh` must not contain unescaped single quotes (the script wraps them in single quotes internally).
