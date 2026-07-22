package ru.yandex.practicum.dto;

public record CashResponse(
        String login,
        String type,
        Integer amount,
        Integer newBalance,
        String message
) {
}
