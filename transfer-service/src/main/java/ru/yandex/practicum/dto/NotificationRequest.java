package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.yandex.practicum.enums.NotificationType;

public record NotificationRequest(
        @NotNull(message = "Тип уведомления обязателен")
        NotificationType type,

        @NotBlank(message = "Логин обязателен")
        String login,

        Integer amount,

        @NotBlank(message = "Сообщение обязательно")
        String message
) {
        public NotificationRequest(NotificationEventPayload payload) {
                this(payload.type(), payload.login(), payload.amount(), payload.message());
        }
}