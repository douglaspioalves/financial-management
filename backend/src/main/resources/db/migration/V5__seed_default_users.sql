-- =============================================================================
-- V5__seed_default_users.sql
-- Credenciais de teste para os dois logins iniciais.
-- =============================================================================

INSERT INTO users (id, name, email, password_hash)
VALUES
    ('00000000-0000-0000-0000-000000000011', 'Alice Test', 'alice@example.com', '$2b$12$3syTQ0KaIWAR8m5mAYwgwuirGzJQfV2N6x34smA9pNLD80Rbu9.5e'),
    ('00000000-0000-0000-0000-000000000012', 'Bob Test',   'bob@example.com',   '$2b$12$nu7GlQRsCxBDFA11BIXqz.Qw2iYCp02Dfei3y9zDDKWde3Vjt7SQa')
ON CONFLICT (email) DO NOTHING;
