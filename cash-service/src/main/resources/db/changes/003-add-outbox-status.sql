--liquibase formatted sql

--changeset cash:003
ALTER TABLE cash.outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NEW';

COMMENT ON COLUMN cash.outbox.status IS 'Статус обработки события (NEW, PROCESSING, PROCESSED, FAILED)';

DROP INDEX IF EXISTS cash.idx_outbox_unprocessed;
CREATE INDEX idx_outbox_status_new ON cash.outbox(created_at ASC) WHERE status = 'NEW';