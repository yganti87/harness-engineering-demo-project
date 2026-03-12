-- PostgreSQL Extensions for Library Database
-- This script runs ONCE on first container initialization (before Flyway)
-- Flyway handles all schema creation and seed data.

-- pg_trgm: trigram-based similarity search
-- Used for future fuzzy title/author matching (TD003)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- unaccent: accent-insensitive search
-- Useful for author names with accents (e.g., "Camus" matches "Camùs")
CREATE EXTENSION IF NOT EXISTS unaccent;

-- uuid-ossp: UUID generation functions
-- Used as fallback for gen_random_uuid() if pgcrypto is unavailable
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
