package ru.yandex.practicum.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.entity.TransferOperation;
import ru.yandex.practicum.repository.TransferOperationRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит тесты TransferOperationService")
class TransferOperationServiceTest {

    @Mock
    private TransferOperationRepository transferOperationRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private TransferOperationService transferOperationService;

    @Test
    @DisplayName("saveOperation: должен сохранить операцию и опубликовать 2 outbox-события")
    void saveOperation_ShouldSaveAndPublishTwoEvents() {
        TransferRequest request = new TransferRequest("recipient", 500);

        transferOperationService.saveOperation("sender", request, 500, "saga-123");

        verify(transferOperationRepository).save(any(TransferOperation.class));
        verify(outboxService).publish(eq("TRANSFER_SENT"), any(), eq("notifications-service"), eq("saga-123"));
        verify(outboxService).publish(eq("TRANSFER_RECEIVED"), any(), eq("notifications-service"), eq("saga-123-recv"));
    }
}