--liquibase formatted sql

--changeset cash:002
CREATE TABLE cash.outbox (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    destination VARCHAR(50) NOT NULL,
    saga_id VARCHAR(100),
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    error TEXT
);

CREATE INDEX idx_outbox_unprocessed ON cash.outbox(processed_at) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_saga ON cash.outbox(saga_id);

COMMENT ON TABLE cash.outbox IS 'Outbox для надежной доставки событий';