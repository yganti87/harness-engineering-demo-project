-- V2: Create users table for authentication
-- Managed by Flyway — NEVER modify this file after it has been applied.
-- Passwords stored as BCrypt hashes (never plain text).

CREATE TABLE IF NOT EXISTS users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT users_pkey PRIMARY KEY (id),
    CONSTRAINT users_username_key UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
