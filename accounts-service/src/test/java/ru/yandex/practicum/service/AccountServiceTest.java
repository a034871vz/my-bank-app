package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.AccountUpdateRequest;
import ru.yandex.practicum.entity.Account;
import ru.yandex.practicum.entity.IdempotencyKey;
import ru.yandex.practicum.exception.AccountNotFoundException;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.repository.AccountRepository;
import ru.yandex.practicum.repository.IdempotencyKeyRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_NAME = "Иванов Иван";
    private static final LocalDate TEST_BIRTHDATE = LocalDate.of(2000, 5, 15);

    @BeforeEach
    void setUp() {
        testAccount = new Account(1L, TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE, 1000);
    }


    @Test
    @DisplayName("getByLogin: should return AccountDto when account exists")
    void getByLogin_WhenAccountExists_ShouldReturnAccountDto() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));

        AccountDto result = accountService.getByLogin(TEST_LOGIN);

        assertThat(result.login()).isEqualTo(TEST_LOGIN);
        assertThat(result.name()).isEqualTo(TEST_NAME);
        assertThat(result.balance()).isEqualTo(1000);
        verify(accountRepository).findByLogin(TEST_LOGIN);
    }

    @Test
    @DisplayName("getByLogin: should throw AccountNotFoundException when account not found")
    void getByLogin_WhenAccountNotFound_ShouldThrowException() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getByLogin("unknown"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("update: should update account data successfully")
    void update_WhenValidData_ShouldUpdateAccount() {
        AccountUpdateRequest request = new AccountUpdateRequest("Петров Петр", LocalDate.of(1995, 3, 10));
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.update(TEST_LOGIN, request);

        assertThat(result.name()).isEqualTo("Петров Петр");
        assertThat(testAccount.getName()).isEqualTo("Петров Петр");
        assertThat(testAccount.getBirthdate()).isEqualTo(LocalDate.of(1995, 3, 10));
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("update: should throw ValidationException when age is under 18")
    void update_WhenAgeUnder18_ShouldThrowValidationException() {
        AccountUpdateRequest request = new AccountUpdateRequest("Молодой", LocalDate.now().minusYears(17));
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.update(TEST_LOGIN, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Возраст должен быть старше 18 лет");
    }

    @Test
    @DisplayName("update: should throw AccountNotFoundException when account not found")
    void update_WhenAccountNotFound_ShouldThrowException() {
        AccountUpdateRequest request = new AccountUpdateRequest("Имя", LocalDate.of(1990, 1, 1));
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.update("unknown", request))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("update: should allow exactly 18 years old")
    void update_WhenExactly18YearsOld_ShouldSucceed() {
        LocalDate birthdate18 = LocalDate.now().minusYears(18);
        AccountUpdateRequest request = new AccountUpdateRequest("Взрослый", birthdate18);
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.update(TEST_LOGIN, request);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAllForTransfer: should return list of all accounts")
    void getAllForTransfer_ShouldReturnAllAccounts() {
        Account account2 = new Account(2L, "user2", "User Two", LocalDate.of(1998, 1, 1), 500);
        when(accountRepository.findAll()).thenReturn(List.of(testAccount, account2));

        List<AccountDto> result = accountService.getAllForTransfer();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).login()).isEqualTo(TEST_LOGIN);
        assertThat(result.get(1).login()).isEqualTo("user2");
    }

    @Test
    @DisplayName("getAllForTransfer: should return empty list when no accounts")
    void getAllForTransfer_WhenNoAccounts_ShouldReturnEmptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<AccountDto> result = accountService.getAllForTransfer();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("changeBalance: should increase balance successfully")
    void changeBalance_WhenDeposit_ShouldIncreaseBalance() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.changeBalance(TEST_LOGIN, 500, null);

        assertThat(result.balance()).isEqualTo(1500);
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeBalance: should decrease balance successfully")
    void changeBalance_WhenWithdraw_ShouldDecreaseBalance() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.changeBalance(TEST_LOGIN, -300, null);

        assertThat(result.balance()).isEqualTo(700);
    }

    @Test
    @DisplayName("changeBalance: should throw ValidationException when insufficient funds")
    void changeBalance_WhenInsufficientFunds_ShouldThrowException() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.changeBalance(TEST_LOGIN, -1500, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Недостаточно средств на счету");
    }

    @Test
    @DisplayName("changeBalance: should throw ValidationException when balance goes to zero after withdrawal")
    void changeBalance_WhenWithdrawExactAmount_ShouldSucceed() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.changeBalance(TEST_LOGIN, -1000, null);

        assertThat(result.balance()).isEqualTo(0);
    }

    @Test
    @DisplayName("changeBalance: should throw ValidationException when balance goes negative")
    void changeBalance_WhenBalanceGoesNegative_ShouldThrowException() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));

        assertThatThrownBy(() -> accountService.changeBalance(TEST_LOGIN, -1001, null))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Недостаточно средств на счету");
    }

    @Test
    @DisplayName("changeBalance: should save idempotency key when provided")
    void changeBalance_WhenIdempotencyKeyProvided_ShouldSaveKey() {
        String key = "key-123";
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(idempotencyKeyRepository.findByKey(key)).thenReturn(Optional.empty());

        AccountDto result = accountService.changeBalance(TEST_LOGIN, 200, key);

        assertThat(result.balance()).isEqualTo(1200);
        verify(idempotencyKeyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    @DisplayName("changeBalance: should return cached result on duplicate idempotency key")
    void changeBalance_WhenDuplicateIdempotencyKey_ShouldReturnCachedResult() {
        String key = "key-duplicate";
        IdempotencyKey cachedKey = new IdempotencyKey(key, TEST_LOGIN, 5000);
        Account cachedAccount = new Account(1L, TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE, 5000);

        when(idempotencyKeyRepository.findByKey(key)).thenReturn(Optional.of(cachedKey));
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(cachedAccount));

        AccountDto result = accountService.changeBalance(TEST_LOGIN, 200, key);

        assertThat(result.balance()).isEqualTo(5000);
        verify(accountRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    @DisplayName("changeBalance: should not check idempotency key when blank")
    void changeBalance_WhenBlankIdempotencyKey_ShouldIgnoreKey() {
        when(accountRepository.findByLogin(TEST_LOGIN)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        AccountDto result = accountService.changeBalance(TEST_LOGIN, 100, "   ");

        assertThat(result.balance()).isEqualTo(1100);
        verify(idempotencyKeyRepository, never()).findByKey(any());
    }

    @Test
    @DisplayName("changeBalance: should throw AccountNotFoundException when account not found")
    void changeBalance_WhenAccountNotFound_ShouldThrowException() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.changeBalance("unknown", 100, null))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("ensureAccountExists: should create account when not exists")
    void ensureAccountExists_WhenNotExists_ShouldCreateAccount() {
        when(accountRepository.existsByLogin(TEST_LOGIN)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        accountService.ensureAccountExists(TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE);

        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("ensureAccountExists: should not create account when already exists")
    void ensureAccountExists_WhenExists_ShouldNotCreateAccount() {
        when(accountRepository.existsByLogin(TEST_LOGIN)).thenReturn(true);

        accountService.ensureAccountExists(TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE);

        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("ensureAccountExists: should create account with zero balance")
    void ensureAccountExists_WhenCreating_ShouldHaveZeroBalance() {
        when(accountRepository.existsByLogin(TEST_LOGIN)).thenReturn(false);

        accountService.ensureAccountExists(TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE);

        verify(accountRepository).save(argThat(account ->
                account.getLogin().equals(TEST_LOGIN) &&
                        account.getName().equals(TEST_NAME) &&
                        account.getBirthdate().equals(TEST_BIRTHDATE) &&
                        account.getBalance() == 0
        ));
    }
}