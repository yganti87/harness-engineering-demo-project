# Reliability Guide

## Error Handling

All exceptions bubble up to `GlobalExceptionHandler`. Services throw domain exceptions; controllers never catch exceptions.

**Exception hierarchy**:
- `EntityNotFoundException` (extends `RuntimeException`) → 404
- `MethodArgumentNotValidException` (Spring) → 400
- `ConstraintViolationException` (Jakarta) → 400
- `Exception` (catch-all) → 500

**Rule**: Never swallow exceptions silently. Never return null from service methods (use Optional or throw).

## Structured Logging

Backend uses JSON structured logging via Logback. Every log entry includes:
- `@timestamp` — ISO 8601
- `level` — INFO / WARN / ERROR / DEBUG
- `logger_name` — fully qualified class name
- `message` — human-readable
- `stack_trace` — only on ERROR level

**Rule**: Use SLF4J placeholders, never string concatenation:
```java
// CORRECT
log.info("Searching books query='{}' genre='{}'", query, genre);

// WRONG: string concat creates strings even when log level is disabled
log.info("Searching books query='" + query + "' genre='" + genre + "'");
```

**Log levels**:
| Level | When to use |
|-------|------------|
| ERROR | Unexpected failures that need investigation |
| WARN | Expected failures (book not found, validation error) |
| INFO | Business operations (search request, result count) |
| DEBUG | Diagnostic details (SQL parameters, timing) — disabled in production |

## Health Checks

Spring Boot Actuator provides health indicators:
- `db` — verifies database connection
- `diskSpace` — verifies sufficient disk space

Docker compose uses `/actuator/health` for container health checks.

Endpoint: `GET /actuator/health`

## Logging Access for Agents

Agents can access logs via:

1. **Host file** (JSON, structured):
   ```bash
   tail -50 ./logs/backend/app.log
   ```

2. **HTTP endpoint**:
   ```bash
   curl http://localhost:8080/actuator/logfile | tail -50
   ```

3. **Script**:
   ```bash
   ./scripts/logs.sh backend 50
   ./scripts/logs.sh frontend 50
   ```

4. **Docker**:
   ```bash
   docker compose logs backend --follow --tail=20
   ```

## Resilience Patterns

- **Database connection pool**: HikariCP (Spring Boot default), pool size 10
- **Connection timeout**: 30s
- **Flyway**: runs on startup, blocks app start until migrations complete
- **Testcontainers**: integration tests use isolated PostgreSQL — never test against shared DB

## Monitoring

- `GET /actuator/metrics` — JVM, HTTP, DB metrics
- `GET /actuator/info` — app version, git commit (if configured)

When adding new features, add a meaningful log at INFO level for the primary business operation (e.g., "Book search completed results={} query='{}'").
