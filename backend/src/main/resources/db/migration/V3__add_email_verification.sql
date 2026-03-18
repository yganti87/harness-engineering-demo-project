-- V3: Replace username with email, add email verification
-- Managed by Flyway — NEVER modify this file after it has been applied.

ALTER TABLE users RENAME COLUMN username TO email;
ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(255);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
DROP INDEX IF EXISTS idx_users_username;
CREATE INDEX idx_users_email ON users (email);
ALTER TABLE users DROP CONSTRAINT users_username_key;
ALTER TABLE users ADD CONSTRAINT users_email_key UNIQUE (email);

CREATE TABLE email_verification_tokens (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT evt_pkey PRIMARY KEY (id),
    CONSTRAINT evt_token_key UNIQUE (token),
    CONSTRAINT evt_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_evt_token ON email_verification_tokens (token);
CREATE INDEX idx_evt_user_id ON email_verification_tokens (user_id);
