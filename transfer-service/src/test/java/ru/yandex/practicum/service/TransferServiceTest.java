package ru.yandex.practicum.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.dto.TransferResponse;
import ru.yandex.practicum.exception.ValidationException;

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
@DisplayName("Юнит тесты TransferService")
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private TransferOperationService transferOperationService;

    @Mock
    private SagaCompensator sagaCompensator;

    @InjectMocks
    private TransferService transferService;

    private static final String SENDER = "sender";
    private static final String RECIPIENT = "recipient";

    @Test
    @DisplayName("process: должен успешно выполнить перевод")
    void process_Success() {
        TransferRequest request = new TransferRequest(RECIPIENT, 500);
        AccountDto senderAfter = new AccountDto(SENDER, 500);
        AccountDto recipientAfter = new AccountDto(RECIPIENT, 1500);

        when(accountsClient.changeBalance(eq(SENDER), eq(-500), anyString())).thenReturn(senderAfter);
        when(accountsClient.changeBalance(eq(RECIPIENT), eq(500), anyString())).thenReturn(recipientAfter);

        TransferResponse response = transferService.process(SENDER, request);

        assertThat(response.senderLogin()).isEqualTo(SENDER);
        assertThat(response.recipientLogin()).isEqualTo(RECIPIENT);
        assertThat(response.amount()).isEqualTo(500);
        assertThat(response.senderNewBalance()).isEqualTo(500);
        assertThat(response.message()).contains("Перевод 500 руб");

        verify(transferOperationService).saveOperation(eq(SENDER), eq(request), eq(500), anyString());
        verify(sagaCompensator, never()).compensate(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("process: должен выбросить ValidationException при переводе самому себе")
    void process_SelfTransfer_ShouldThrow() {
        TransferRequest request = new TransferRequest(SENDER, 500);

        assertThatThrownBy(() -> transferService.process(SENDER, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Нельзя переводить самому себе");
    }

    @Test
    @DisplayName("process: должен компенсировать при ошибке после списания")
    void process_WhenDepositFails_ShouldCompensateSender() {
        TransferRequest request = new TransferRequest(RECIPIENT, 500);
        AccountDto senderAfter = new AccountDto(SENDER, 500);

        when(accountsClient.changeBalance(eq(SENDER), eq(-500), anyString())).thenReturn(senderAfter);
        when(accountsClient.changeBalance(eq(RECIPIENT), eq(500), anyString()))
                .thenThrow(new RuntimeException("Recipient not found"));

        assertThatThrownBy(() -> transferService.process(SENDER, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Перевод не выполнен");

        verify(sagaCompensator).compensate(eq(SENDER), eq(500), anyString());
    }

    @Test
    @DisplayName("process: должен компенсировать обоих при ошибке сохранения операции")
    void process_WhenSaveOperationFails_ShouldCompensateBoth() {
        TransferRequest request = new TransferRequest(RECIPIENT, 500);
        AccountDto senderAfter = new AccountDto(SENDER, 500);
        AccountDto recipientAfter = new AccountDto(RECIPIENT, 1500);

        when(accountsClient.changeBalance(eq(SENDER), eq(-500), anyString())).thenReturn(senderAfter);
        when(accountsClient.changeBalance(eq(RECIPIENT), eq(500), anyString())).thenReturn(recipientAfter);
        doThrow(new RuntimeException("DB error")).when(transferOperationService)
                .saveOperation(any(), any(), anyInt(), anyString());

        assertThatThrownBy(() -> transferService.process(SENDER, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Перевод не выполнен");

        verify(sagaCompensator).compensate(eq(SENDER), eq(500), anyString());
        verify(sagaCompensator).compensate(eq(RECIPIENT), eq(-500), anyString());
    }

    @Test
    @DisplayName("process: не должен компенсировать если ошибка на первом шаге")
    void process_WhenWithdrawFails_ShouldNotCompensate() {
        TransferRequest request = new TransferRequest(RECIPIENT, 500);

        when(accountsClient.changeBalance(eq(SENDER), eq(-500), anyString()))
                .thenThrow(new RuntimeException("Insufficient funds"));

        assertThatThrownBy(() -> transferService.process(SENDER, request))
                .isInstanceOf(ValidationException.class);

        verify(sagaCompensator, never()).compensate(any(), anyInt(), anyString());
    }
}