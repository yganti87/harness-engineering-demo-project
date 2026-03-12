FROM postgres:15-alpine

# Initialization scripts are mounted via docker-compose.yml
# from ./database/init/ to /docker-entrypoint-initdb.d/
# They run automatically on first container start (before Flyway).

# The actual schema is managed by Flyway (backend startup).
# This image only handles PostgreSQL extensions.

EXPOSE 5432
