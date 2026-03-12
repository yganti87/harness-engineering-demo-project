#!/usr/bin/env bash
# run-tests.sh: Run the full test suite.
#
# Runs in order:
#   1. Checkstyle (code style)
#   2. Unit + architecture tests (no Docker needed)
#   3. Integration tests (requires Docker for Testcontainers)
#
# Usage:
#   ./scripts/run-tests.sh              # full suite
#   ./scripts/run-tests.sh --unit-only  # skip integration tests

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
UNIT_ONLY="${1:-}"

cd "$PROJECT_DIR/backend"

echo "======================================================"
echo " Library Backend Test Suite"
echo "======================================================"
echo ""

# ── 1. Checkstyle ──────────────────────────────────────────────────────────
echo "── Step 1: Checkstyle (code style) ──────────────────"
mvn checkstyle:check -q
echo "    ✓ Checkstyle passed"
echo ""

# ── 2. Unit + Architecture Tests ──────────────────────────────────────────
echo "── Step 2: Unit + Architecture tests ────────────────"
mvn test -Dgroups='!integration' -q
echo "    ✓ Unit and architecture tests passed"
echo ""

# ── 3. Integration Tests ──────────────────────────────────────────────────
if [ "$UNIT_ONLY" = "--unit-only" ]; then
    echo "── Step 3: Integration tests SKIPPED (--unit-only)"
    echo ""
else
    echo "── Step 3: Integration tests (requires Docker) ──────"
    if ! docker info > /dev/null 2>&1; then
        echo "    ✗ Docker is not running. Start Docker Desktop and retry."
        exit 1
    fi
    mvn test -Dgroups=integration -q
    echo "    ✓ Integration tests passed"
    echo ""
fi

echo "======================================================"
echo " All tests passed!"
echo "======================================================"
echo ""
echo "Quick commands:"
echo "  Unit only:         cd backend && mvn test -Dgroups='!integration'"
echo "  Integration only:  cd backend && mvn test -Dgroups=integration"
echo "  Specific test:     cd backend && mvn test -Dtest=BookServiceTest"
echo "  Frontend tests:    cd frontend && python -m pytest tests/ -v"
