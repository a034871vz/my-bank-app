package ru.yandex.practicum.dto;

public record TransferResponse(
        String senderLogin,
        String recipientLogin,
        Integer amount,
        Integer senderNewBalance,
        String message
) {
}