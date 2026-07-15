package ru.yandex.practicum.dto;

public record NotificationRequest(
        String type,
        String login,
        Integer amount,
        String message
) {
}
