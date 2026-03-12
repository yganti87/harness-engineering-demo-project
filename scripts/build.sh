#!/usr/bin/env bash
# build.sh: Build all Docker images without starting services.
#
# Usage: ./scripts/build.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "==> Creating log directories..."
mkdir -p ./logs/backend ./logs/frontend

echo ""
echo "==> Building Docker images..."
docker compose build

echo ""
echo "==> Build complete."
echo "    To start: ./scripts/start.sh"
echo "    To test:  ./scripts/run-tests.sh"
