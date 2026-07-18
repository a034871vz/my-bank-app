package ru.yandex.practicum.mybankfront.dto;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        Integer balance
) {
}