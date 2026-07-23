package ru.yandex.practicum.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.entity.CashOperation;
import ru.yandex.practicum.enums.CashOperationType;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.repository.CashOperationRepository;
import ru.yandex.practicum.service.CashOperationService;
import ru.yandex.practicum.service.CashService;
import ru.yandex.practicum.service.OutboxService;
import ru.yandex.practicum.service.SagaCompensator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит тесты CashOperationService")
class CashOperationServiceTest {

    @Mock
    private CashOperationRepository cashOperationRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CashOperationService cashOperationService;

    private static final String LOGIN = "testuser";

    @Test
    @DisplayName("saveOperation: должен сохранить операцию и опубликовать outbox для DEPOSIT")
    void saveOperation_Deposit_ShouldSaveAndPublish() {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 500);
        int newBalance = 1500;
        String sagaId = "saga-123";

        cashOperationService.saveOperation(LOGIN, request, newBalance, sagaId);

        verify(cashOperationRepository).save(any(CashOperation.class));
        verify(outboxService).publish(
                eq("CASH_DEPOSIT"),
                any(),
                eq("notifications-service"),
                eq(sagaId)
        );
    }

    @Test
    @DisplayName("saveOperation: должен сохранить операцию и опубликовать outbox для WITHDRAW")
    void saveOperation_Withdraw_ShouldSaveAndPublish() {
        CashRequest request = new CashRequest(CashOperationType.WITHDRAW, 300);
        int newBalance = 700;
        String sagaId = "saga-456";

        cashOperationService.saveOperation(LOGIN, request, newBalance, sagaId);

        verify(cashOperationRepository).save(any(CashOperation.class));
        verify(outboxService).publish(
                eq("CASH_WITHDRAW"),
                any(),
                eq("notifications-service"),
                eq(sagaId)
        );
    }
}