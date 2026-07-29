package ru.yandex.practicum.dto;

import ru.yandex.practicum.enums.NotificationType;

public record NotificationEventPayload(
        NotificationType type,
        String login,
        Integer amount,
        String message,
        Integer newBalance,
        String sagaId
) {
}