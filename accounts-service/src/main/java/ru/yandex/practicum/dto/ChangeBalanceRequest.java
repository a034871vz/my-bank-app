package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeBalanceRequest(
        @NotNull(message = "Сумма обязательна")
        Integer amount,

        String idempotencyKey
) {
}