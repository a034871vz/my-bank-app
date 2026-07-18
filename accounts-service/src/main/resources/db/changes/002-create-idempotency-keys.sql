--liquibase formatted sql

--changeset accounts:002
CREATE TABLE accounts.idempotency_keys (
    key VARCHAR(100) PRIMARY KEY,
    login VARCHAR(50) NOT NULL,
    resulting_balance INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_idempotency_keys_login ON accounts.idempotency_keys(login);