package ru.yandex.practicum.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {
    private final String login;

    public AccountNotFoundException(String login) {
        super("Аккаунт не найден: " + login);
        this.login = login;
    }
}