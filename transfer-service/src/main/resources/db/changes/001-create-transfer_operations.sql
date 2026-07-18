--liquibase formatted sql

--changeset transfer:001
CREATE TABLE transfer.transfer_operations (
    id BIGSERIAL PRIMARY KEY,
    sender_login VARCHAR(50) NOT NULL,
    recipient_login VARCHAR(50) NOT NULL,
    amount INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE transfer.transfer_operations IS 'История переводов между пользователями';