# Task: Enable Claude Code Preview Screenshots for Streamlit Frontend

**Task ID**: T002
**Status**: completed
**Scope**: infra

---

## Context

Claude Code's Preview tool (`preview_start`, `preview_screenshot`) runs a headless browser inside a sandbox that cannot reach Docker's localhost ports. The Streamlit frontend was configured to run only as a Docker container (port 8501), making it unreachable for screenshots. Running Streamlit natively also failed because it attempted to write logs to `/var/log/app`, a path that exists only inside the container.

---

## Description

- [x] Update `.ai/launch.json` — Replace the `frontend` configuration's Docker command with a call to a new bash wrapper script; add `BACKEND_URL` and `LOG_DIR` env vars to the config.
- [x] Create `.ai/scripts/start-frontend-preview.sh` — A wrapper that:
  - Starts backend and database Docker services in detached mode (`docker compose up -d db backend`)
  - Polls `${BACKEND_URL}/actuator/health` every 2 seconds for up to 60 seconds before proceeding
  - Launches Streamlit natively using the pyenv virtualenv binary with `--server.headless true`
  - Inherits `LOG_DIR=./logs/frontend` from the launch config to redirect logs away from the Docker-only `/var/log/app` path

---

## Acceptance Criteria

1. `preview_start("frontend")` completes without error.
2. `preview_screenshot` returns a fully rendered Library Catalog UI showing books fetched from the backend (no "Cannot connect to backend" error visible).
3. Starting the frontend natively does not produce `PermissionError: [Errno 13] Permission denied: '/var/log/app'`.
4. Backend and database Docker services are started automatically by the script if not already running.
5. If the backend does not become healthy within 60 seconds, the script emits a warning and starts the frontend anyway (non-fatal).

---

## Verification

| Check | Command / Action |
|-------|------------------|
| Script is executable | `ls -l .ai/scripts/start-frontend-preview.sh` — should show execute bit |
| Launch config is valid JSON | `python3 -m json.tool .ai/launch.json` |
| Manual — preview start | `preview_start("frontend")` succeeds in Claude Code |
| Manual — screenshot | `preview_screenshot` shows a rendered Library Catalog page with book listings |
| Manual — no permission error | No `PermissionError: /var/log/app` in Streamlit output |

---

## Out of Scope

- Changes to the frontend application code (`frontend/app.py` or related modules).
- Changes to the Docker Compose setup for normal (non-preview) frontend operation.
- Making the pyenv virtualenv path configurable; the path is currently hardcoded to the local developer environment.
- CI/CD pipeline changes.

---

## Files Touched

- `.ai/launch.json` (modified — frontend config updated to use wrapper script)
- `.ai/scripts/start-frontend-preview.sh` (new)
