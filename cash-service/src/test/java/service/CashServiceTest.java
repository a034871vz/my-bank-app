package service;

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
import ru.yandex.practicum.enums.CashOperationType;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.service.CashOperationService;
import ru.yandex.practicum.service.CashService;
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
@DisplayName("Юнит тесты CashService")
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private CashOperationService cashOperationService;

    @Mock
    private SagaCompensator sagaCompensator;

    @InjectMocks
    private CashService cashService;

    private static final String LOGIN = "testuser";

    @Test
    @DisplayName("process: должен успешно выполнить DEPOSIT")
    void process_Deposit_Success() {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 500);
        AccountDto accountDto = new AccountDto(LOGIN, 1500);

        when(accountsClient.changeBalance(eq(LOGIN), eq(500), anyString()))
                .thenReturn(accountDto);

        CashResponse response = cashService.process(LOGIN, request);

        assertThat(response.type()).isEqualTo("DEPOSIT");
        assertThat(response.amount()).isEqualTo(500);
        assertThat(response.newBalance()).isEqualTo(1500);
        assertThat(response.message()).isEqualTo("Положено 500 руб");

        verify(cashOperationService).saveOperation(eq(LOGIN), eq(request), eq(1500), anyString());
        verify(sagaCompensator, never()).compensate(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("process: должен успешно выполнить WITHDRAW")
    void process_Withdraw_Success() {
        CashRequest request = new CashRequest(CashOperationType.WITHDRAW, 300);
        AccountDto accountDto = new AccountDto(LOGIN, 700);

        when(accountsClient.changeBalance(eq(LOGIN), eq(-300), anyString()))
                .thenReturn(accountDto);

        CashResponse response = cashService.process(LOGIN, request);

        assertThat(response.type()).isEqualTo("WITHDRAW");
        assertThat(response.amount()).isEqualTo(300);
        assertThat(response.newBalance()).isEqualTo(700);
        assertThat(response.message()).isEqualTo("Снято 300 руб");

        verify(cashOperationService).saveOperation(eq(LOGIN), eq(request), eq(700), anyString());
        verify(sagaCompensator, never()).compensate(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("process: должен выполнить компенсацию при ошибке после изменения баланса")
    void process_WhenAccountsClientThrowsAfterBalanceChange_ShouldCompensate() {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 500);
        when(accountsClient.changeBalance(eq(LOGIN), eq(500), anyString()))
                .thenReturn(new AccountDto(LOGIN, 1500));

        doThrow(new RuntimeException("Ошибка сохранения операции"))
                .when(cashOperationService).saveOperation(any(), any(), anyInt(), anyString());

        assertThatThrownBy(() -> cashService.process(LOGIN, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Операция не выполнена");

        verify(sagaCompensator).compensate(eq(LOGIN), eq(-500), anyString());
    }

    @Test
    @DisplayName("process: не должен вызывать компенсацию, если ошибка при вызове AccountsClient (баланс не изменился)")
    void process_WhenAccountsClientFails_ShouldNotCompensate() {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 500);

        when(accountsClient.changeBalance(eq(LOGIN), eq(500), anyString()))
                .thenThrow(new RuntimeException("Accounts service unavailable"));

        assertThatThrownBy(() -> cashService.process(LOGIN, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Операция не выполнена");

        verify(cashOperationService, never()).saveOperation(any(), any(), anyInt(), anyString());
        verify(sagaCompensator, never()).compensate(any(), anyInt(), anyString());
    }
}