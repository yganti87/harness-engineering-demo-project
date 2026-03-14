#!/usr/bin/env bash
# Starts the Streamlit frontend for Claude Preview, waiting for the backend first.
# Usage: Called by launch.json "frontend" configuration.

set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
HEALTH_ENDPOINT="${BACKEND_URL}/actuator/health"
MAX_WAIT=60
INTERVAL=2

# Ensure backend Docker services are running
docker compose up -d db backend 2>/dev/null || true

echo "Waiting for backend at ${HEALTH_ENDPOINT} (up to ${MAX_WAIT}s)..."
elapsed=0
while [ "$elapsed" -lt "$MAX_WAIT" ]; do
    if curl -sf "$HEALTH_ENDPOINT" >/dev/null 2>&1; then
        echo "Backend is ready."
        break
    fi
    sleep "$INTERVAL"
    elapsed=$((elapsed + INTERVAL))
done

if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo "WARNING: Backend not ready after ${MAX_WAIT}s, starting frontend anyway."
fi

exec /Users/yash/.pyenv/versions/data-agent-virtualenv/bin/streamlit run frontend/app.py --server.headless true
