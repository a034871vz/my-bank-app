package ru.yandex.practicum.dto;

import ru.yandex.practicum.entity.Account;

public record AccountDto(
        String login,
        String name,
        String birthdate,
        Integer balance
) {
    public AccountDto(Account account) {
        this(account.getLogin(), account.getName(), account.getBirthdate().toString(), account.getBalance());
    }
}