#!/usr/bin/env bash
# logs.sh: Show recent logs for a service.
#
# Usage:
#   ./scripts/logs.sh                    # backend, last 50 lines
#   ./scripts/logs.sh backend            # backend logs
#   ./scripts/logs.sh backend 100        # backend, last 100 lines
#   ./scripts/logs.sh frontend 50        # frontend logs
#   ./scripts/logs.sh db                 # database logs
#   ./scripts/logs.sh backend --follow   # follow live

set -euo pipefail

SERVICE="${1:-backend}"
LINES="${2:-50}"
FOLLOW="${3:-}"

case "$SERVICE" in
    backend)
        CONTAINER="library-backend"
        LOG_FILE="./logs/backend/app.log"
        ;;
    frontend)
        CONTAINER="library-frontend"
        LOG_FILE="./logs/frontend/frontend.log"
        ;;
    db|database|postgres)
        CONTAINER="library-db"
        LOG_FILE=""
        ;;
    *)
        echo "Usage: $0 [backend|frontend|db] [lines] [--follow]"
        echo ""
        echo "Examples:"
        echo "  $0 backend 100"
        echo "  $0 frontend 50"
        echo "  $0 db"
        echo "  $0 backend 20 --follow"
        exit 1
        ;;
esac

echo "==> Logs: $SERVICE (last $LINES lines)"
echo "    Container: $CONTAINER"
echo "-----------------------------------------------------------"

if [ "$FOLLOW" = "--follow" ]; then
    docker compose logs "$SERVICE" --follow --tail="$LINES"
elif [ -n "$LOG_FILE" ] && [ -f "$LOG_FILE" ]; then
    echo "    Source: $LOG_FILE (JSON structured)"
    echo ""
    tail -n "$LINES" "$LOG_FILE"
else
    docker compose logs "$SERVICE" --tail="$LINES"
fi
