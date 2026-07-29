--liquibase formatted sql

--changeset transfer:003
ALTER TABLE transfer.outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'NEW';

COMMENT ON COLUMN transfer.outbox.status IS 'Статус обработки события (NEW, PROCESSING, PROCESSED, FAILED)';

DROP INDEX IF EXISTS transfer.idx_outbox_unprocessed;
CREATE INDEX idx_outbox_status_new ON transfer.outbox(created_at ASC) WHERE status = 'NEW';