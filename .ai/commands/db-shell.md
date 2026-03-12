---
description: Open an interactive psql shell in the database container
---

Connect to the PostgreSQL database for direct inspection.

## Command

```bash
docker compose exec db psql -U library_user -d library_db
```

The prompt will be: `library_db=#`
Type `\q` to exit.

## Useful psql Commands

```sql
-- List tables
\dt

-- Describe books table
\d books

-- Count books
SELECT COUNT(*) FROM books;

-- Search books
SELECT id, title, author, genre FROM books WHERE title ILIKE '%spring%';

-- Check migration history
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Check database size
SELECT pg_size_pretty(pg_database_size('library_db'));

-- Show indexes
SELECT indexname, tablename, indexdef FROM pg_indexes WHERE schemaname = 'public';
```

## Non-Interactive Queries

```bash
# Run a single query without entering the shell
docker compose exec db psql -U library_user -d library_db \
    -c "SELECT title, author, genre FROM books ORDER BY title LIMIT 10;"

# Export to CSV
docker compose exec db psql -U library_user -d library_db \
    -c "COPY (SELECT * FROM books) TO STDOUT WITH CSV HEADER;"

# Check if extensions are installed
docker compose exec db psql -U library_user -d library_db \
    -c "SELECT extname FROM pg_extension;"
```
