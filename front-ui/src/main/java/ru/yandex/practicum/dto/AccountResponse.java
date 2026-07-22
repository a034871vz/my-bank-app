package ru.yandex.practicum.dto;

public record AccountResponse(
        String login,
        String name,
        String birthdate,
        Integer balance
) {
}