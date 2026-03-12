---
description: Show recent logs for a service (backend, frontend, or db)
---

Retrieve and display recent log entries for the specified service.

## Usage

```
/logs                 # backend, last 50 lines (default)
/logs backend         # backend logs
/logs backend 100     # backend, last 100 lines
/logs frontend 50     # frontend logs
/logs db              # database logs
```

## Steps

1. If no service specified, default to `backend`
2. Run `./scripts/logs.sh <service> <lines>`
3. For backend logs (JSON): parse and display as table: `timestamp | level | logger | message`
4. Highlight ERROR and WARN lines
5. If log file missing, fall back to Docker container logs

## Reading Backend JSON Logs

```bash
# Human-readable (parse JSON)
tail -50 ./logs/backend/app.log | python3 -c "
import sys, json
for line in sys.stdin:
    try:
        obj = json.loads(line.strip())
        ts = obj.get('@timestamp', '')[:19]
        level = obj.get('level', 'INFO')
        logger = obj.get('logger_name', '')[-30:]
        msg = obj.get('message', '')
        print(f'{ts} [{level:5}] {logger}: {msg}')
    except Exception:
        print(line.rstrip())
"

# Via HTTP (no file access needed)
curl -s http://localhost:8080/actuator/logfile | tail -50

# Follow live backend logs
docker compose logs backend --follow --tail=20

# Follow live all services
docker compose logs --follow --tail=10
```

## Log Levels

| Level | Meaning | Action |
|-------|---------|--------|
| INFO | Normal operation | No action needed |
| WARN | Expected failure (e.g., book not found, validation error) | Verify expected |
| ERROR | Unexpected failure | Investigate immediately |
| DEBUG | Verbose detail | Disabled in production |
