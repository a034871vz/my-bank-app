package ru.yandex.practicum.mybankfront.dto;

public record CashResponse(
        String login,
        String type,
        Integer amount,
        Integer newBalance,
        String message
) {
}
