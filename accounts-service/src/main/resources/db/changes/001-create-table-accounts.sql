--liquibase formatted sql

--changeset accounts:001
CREATE TABLE accounts.accounts (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    birthdate DATE NOT NULL,
    balance INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE accounts.accounts IS 'Банковские аккаунты клиентов';
COMMENT ON COLUMN accounts.accounts.id IS 'Идентификатор аккаунта';
COMMENT ON COLUMN accounts.accounts.login IS 'Логин пользователя (уникальный)';
COMMENT ON COLUMN accounts.accounts.name IS 'Фамилия и имя';
COMMENT ON COLUMN accounts.accounts.birthdate IS 'Дата рождения';
COMMENT ON COLUMN accounts.accounts.balance IS 'Сумма на счету (руб)';
