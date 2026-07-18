package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.AccountUpdateRequest;
import ru.yandex.practicum.entity.Account;
import ru.yandex.practicum.entity.IdempotencyKey;
import ru.yandex.practicum.exception.AccountNotFoundException;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.repository.AccountRepository;
import ru.yandex.practicum.repository.IdempotencyKeyRepository;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    public AccountDto getByLogin(String login) {
        return accountRepository.findByLogin(login)
                .map(AccountDto::new)
                .orElseThrow(() -> {
                    log.warn("Аккаунт не найден: {}", login);
                    return new AccountNotFoundException(login);
                });
    }

    @Transactional
    public AccountDto update(String login, AccountUpdateRequest request) {
        Account account = accountRepository.findByLogin(login).orElseThrow(() -> new AccountNotFoundException(login));

        validateAge(request.birthdate());
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        return new AccountDto(accountRepository.save(account));
    }

    public List<AccountDto> getAllForTransfer() {
        return accountRepository.findAll().stream().map(AccountDto::new).toList();
    }

    @Transactional
    public AccountDto changeBalance(String login, int amount, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyKeyRepository.findByKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Повторный запрос с key={}, возвращаем кэшированный результат", idempotencyKey);
                return accountRepository.findByLogin(login)
                        .map(AccountDto::new)
                        .orElseThrow(() -> new AccountNotFoundException(login));
            }
        }

        Account account = accountRepository.findByLogin(login).orElseThrow(() -> new AccountNotFoundException(login));

        int newBalance = account.getBalance() + amount;
        if (newBalance < 0) {
            log.warn("Недостаточно средств у {}: баланс {}, запрошено {}", login, account.getBalance(), amount);
            throw new ValidationException("Недостаточно средств на счету");
        }

        account.setBalance(newBalance);
        Account saved = accountRepository.save(account);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyKeyRepository.save(new IdempotencyKey(idempotencyKey, login, saved.getBalance()));
        }

        return new AccountDto(saved);
    }

    @Transactional
    public void ensureAccountExists(String login, String name, LocalDate birthdate) {
        if (!accountRepository.existsByLogin(login)) {
            Account account = new Account(login, name, birthdate, 0);
            accountRepository.save(account);
        }
    }

    private void validateAge(LocalDate birthdate) {
        int age = Period.between(birthdate, LocalDate.now()).getYears();
        if (age < 18) {
            throw new ValidationException("Возраст должен быть старше 18 лет");
        }
    }
}