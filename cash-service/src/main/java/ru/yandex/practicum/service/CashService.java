package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.exception.ValidationException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;
    private final CashOperationService cashOperationService;
    private final SagaCompensator sagaCompensator;

    public CashResponse process(String login, CashRequest request) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Saga {}: операция {} для {} на сумму {}", sagaId, request.type(), login, request.amount());

        int amountDelta = calculateAmountDelta(request);
        AccountDto accountDto = null;

        try {
            accountDto = accountsClient.changeBalance(login, amountDelta, sagaId);
            cashOperationService.saveOperation(login, request, accountDto.balance(), sagaId);

            String message = buildMessage(request);
            return new CashResponse(login, request, accountDto, message);

        } catch (Exception e) {
            log.error("Saga {} упала: {}", sagaId, e.getMessage());

            if (accountDto != null) {
                sagaCompensator.compensate(login, -amountDelta, sagaId + "-compensation");
            }

            throw new ValidationException("Операция не выполнена: " + e.getMessage());
        }
    }

    private int calculateAmountDelta(CashRequest request) {
        return switch (request.type()) {
            case DEPOSIT -> request.amount();
            case WITHDRAW -> -request.amount();
        };
    }

    private String buildMessage(CashRequest request) {
        return switch (request.type()) {
            case DEPOSIT -> "Положено " + request.amount() + " руб";
            case WITHDRAW -> "Снято " + request.amount() + " руб";
        };
    }
}