--liquibase formatted sql

--changeset cash:001
CREATE TABLE cash.cash_operations (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    amount INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE cash.cash_operations IS 'Операции пополнения и снятия наличных';
COMMENT ON COLUMN cash.cash_operations.id IS 'Идентификатор операции';
COMMENT ON COLUMN cash.cash_operations.login IS 'Логин пользователя';
COMMENT ON COLUMN cash.cash_operations.operation_type IS 'Тип операции (DEPOSIT/WITHDRAW)';
COMMENT ON COLUMN cash.cash_operations.amount IS 'Сумма операции';
COMMENT ON COLUMN cash.cash_operations.created_at IS 'Дата и время операции';
