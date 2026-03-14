#!/usr/bin/env bash
# start.sh: Build and start all library services via Docker Compose.
#
# Usage: ./scripts/start.sh
#
# Steps:
#   1. Create log directories on host
#   2. Copy .env.example to .env if .env missing
#   3. Build and start all containers in detached mode
#   4. Wait for backend health check to pass

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "==> Preparing environment..."

mkdir -p ./logs/backend
mkdir -p ./logs/frontend

if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "    Created .env from .env.example. Review values before going to production."
    else
        echo "    WARNING: No .env or .env.example found."
    fi
fi

echo "==> Building and starting services..."
docker compose up --build -d

echo ""
echo "==> Waiting for backend to become healthy (up to 120s)..."

TIMEOUT=120
ELAPSED=0
INTERVAL=5

while [ $ELAPSED -lt $TIMEOUT ]; do
    STATUS=$(docker inspect --format='{{.State.Health.Status}}' library-backend 2>/dev/null || echo "starting")
    if [ "$STATUS" = "healthy" ]; then
        echo ""
        echo "┌──────────────────────────────────────────────────────────┐"
        echo "│  Library App is running!                                 │"
        echo "├──────────────────────────────────────────────────────────┤"
        echo "│  Backend API:   http://localhost:8080                    │"
        echo "│  Swagger UI:    http://localhost:8080/swagger-ui.html    │"
        echo "│  Health:        http://localhost:8080/actuator/health    │"
        echo "│  Frontend:      http://localhost:8501                    │"
        echo "│  Prometheus:    http://localhost:9090                    │"
        echo "│  Grafana:       http://localhost:3000  (admin/admin)     │"
        echo "│  Database:      localhost:5433 (PostgreSQL)              │"
        echo "├──────────────────────────────────────────────────────────┤"
        echo "│  Logs:          ./logs/backend/app.log                   │"
        echo "│  Log via HTTP:  curl localhost:8080/actuator/logfile     │"
        echo "│  Live logs:     ./scripts/logs.sh backend                │"
        echo "└──────────────────────────────────────────────────────────┘"
        exit 0
    fi
    printf "."
    sleep $INTERVAL
    ELAPSED=$((ELAPSED + INTERVAL))
done

echo ""
echo "    WARNING: Backend did not become healthy within ${TIMEOUT}s."
echo "    Check logs: ./scripts/logs.sh backend"
echo "    Or: docker compose logs backend --tail=50"
exit 1
