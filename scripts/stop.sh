#!/usr/bin/env bash
# stop.sh: Stop all library services.
#
# Usage:
#   ./scripts/stop.sh         # Stop containers (keep data volumes)
#   ./scripts/stop.sh --clean # Stop and remove data volumes

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

CLEAN=${1:-}

if [ "$CLEAN" = "--clean" ]; then
    echo "==> Stopping services and removing volumes..."
    docker compose down -v
    echo "    Data volumes removed."
else
    echo "==> Stopping services (data preserved)..."
    docker compose down
    echo "    To also remove data volumes: ./scripts/stop.sh --clean"
fi

echo "==> Done."
