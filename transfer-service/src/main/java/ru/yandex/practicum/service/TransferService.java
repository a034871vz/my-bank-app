package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.dto.TransferResponse;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.exception.ValidationException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;
    private final TransferOperationService transferOperationService;
    private final SagaCompensator sagaCompensator;

    public TransferResponse process(String senderLogin, TransferRequest request) {
        String sagaId = UUID.randomUUID().toString();
        String recipientLogin = request.recipientLogin();
        int amount = request.amount();

        log.info("Saga {}: перевод {} руб от {} к {}", sagaId, amount, senderLogin, recipientLogin);

        if (senderLogin.equals(recipientLogin)) {
            throw new ValidationException("Нельзя переводить самому себе");
        }

        AccountDto senderAfterWithdraw = null;
        AccountDto recipientAfterDeposit = null;

        try {
            senderAfterWithdraw = accountsClient.changeBalance(senderLogin, -amount, sagaId + "-withdraw");
            recipientAfterDeposit = accountsClient.changeBalance(recipientLogin, amount, sagaId + "-deposit");

            transferOperationService.saveOperation(senderLogin, request, senderAfterWithdraw.balance(), sagaId);

            return new TransferResponse(senderLogin, recipientLogin, amount, senderAfterWithdraw.balance(),
                    "Перевод " + amount + " руб пользователю " + recipientLogin
            );

        } catch (Exception e) {
            log.error("Saga {} упала: {}", sagaId, e.getMessage());

            if (senderAfterWithdraw != null) {
                sagaCompensator.compensate(senderLogin, amount, sagaId + "-compensation-sender");
            }
            if (recipientAfterDeposit != null) {
                sagaCompensator.compensate(recipientLogin, -amount, sagaId + "-compensation-recipient");
            }

            throw new ValidationException("Перевод не выполнен: " + e.getMessage());
        }
    }
}