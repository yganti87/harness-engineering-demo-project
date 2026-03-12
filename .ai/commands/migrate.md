---
description: Check Flyway migration status or apply pending migrations
---

Manage Flyway database migrations.

## Steps

1. Check if database container is running
2. Show current migration status
3. Apply any pending migrations if requested

## Commands

```bash
# Check migration status via Actuator (requires running backend)
curl -s http://localhost:8080/actuator/flyway | python3 -m json.tool

# Check via psql directly
docker compose exec db psql -U library_user -d library_db \
    -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"

# Trigger migration by restarting backend (Flyway runs on startup)
docker compose restart backend

# Run migration directly via Maven (useful in CI, backend must NOT be running)
cd backend && mvn flyway:migrate \
    -Dflyway.url=jdbc:postgresql://localhost:5433/library_db \
    -Dflyway.user=library_user \
    -Dflyway.password=library_pass
```

## Creating a New Migration

New migration files MUST follow this naming pattern:
```
backend/src/main/resources/db/migration/V{n}__description_with_underscores.sql
```

Examples:
- `V2__add_isbn_unique_index.sql`
- `V3__add_book_availability_column.sql`
- `V4__create_users_table.sql`

**CRITICAL**: Never modify existing migration files. Flyway checks checksums.
Modifying an applied migration causes `FlywayException: Migration checksum mismatch`.
See docs/COMMON_PITFALLS.md P004.

## After Adding a Migration

1. Create the new SQL file in `db/migration/`
2. Update `docs/generated/db-schema.md` to reflect the new schema
3. Restart backend: `docker compose restart backend`
4. Verify: `curl http://localhost:8080/actuator/flyway`
