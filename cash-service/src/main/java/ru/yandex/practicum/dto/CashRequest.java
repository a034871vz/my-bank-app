package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.yandex.practicum.enums.CashOperationType;

public record CashRequest(
        @NotNull(message = "Тип операции обязателен")
        CashOperationType type,

        @NotNull(message = "Сумма обязательна")
        @Positive(message = "Сумма должна быть положительной")
        Integer amount
) {
}