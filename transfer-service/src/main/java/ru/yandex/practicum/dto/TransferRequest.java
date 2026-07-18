package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank(message = "Логин получателя обязателен")
        String recipientLogin,

        @NotNull(message = "Сумма обязательна")
        @Positive(message = "Сумма должна быть положительной")
        Integer amount
) {
}