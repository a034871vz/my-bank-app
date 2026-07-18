package ru.yandex.practicum.dto;

public record CashResponse(
        String login,
        String type,
        Integer amount,
        Integer newBalance,
        String message
) {
    public CashResponse(String login, CashRequest request, AccountDto accountDto, String message) {
        this(login, request.type().name(), request.amount(), accountDto != null ? accountDto.balance() : null, message);
    }
}