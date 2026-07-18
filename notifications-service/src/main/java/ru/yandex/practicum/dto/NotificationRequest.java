package ru.yandex.practicum.dto;

import ru.yandex.practicum.enums.NotificationType;

public record NotificationRequest(
        NotificationType type,
        String login,
        Integer amount,
        String message
) {
}
