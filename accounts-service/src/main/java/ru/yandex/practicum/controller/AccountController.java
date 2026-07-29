package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.AccountUpdateRequest;
import ru.yandex.practicum.dto.ChangeBalanceRequest;
import ru.yandex.practicum.service.AccountService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String CLAIM_USERNAME = "preferred_username";
    private static final String CLAIM_GIVEN_NAME = "given_name";
    private static final String CLAIM_FAMILY_NAME = "family_name";
    private static final String CLAIM_NAME = "name";
    private static final String DEFAULT_USER_NAME = "Неизвестный пользователь";
    private static final LocalDate DEFAULT_BIRTHDATE = LocalDate.of(2001, 1, 1);

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<AccountDto> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getClaimAsString(CLAIM_USERNAME);
        String name = extractNameFromJwt(jwt);

        accountService.ensureAccountExists(login, name, DEFAULT_BIRTHDATE);

        log.info("Пользователь {} запросил данные аккаунта", login);
        return ResponseEntity.ok(accountService.getByLogin(login));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<AccountDto> updateMyAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AccountUpdateRequest request) {
        String login = jwt.getClaimAsString(CLAIM_USERNAME);
        log.info("Пользователь {} обновил данные аккаунта", login);
        return ResponseEntity.ok(accountService.update(login, request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        log.info("Запрошен список аккаунтов для перевода");
        return ResponseEntity.ok(accountService.getAllForTransfer());
    }

    @PostMapping("/{login}/balance")
    @PreAuthorize("hasAuthority('ROLE_accounts')")
    public ResponseEntity<AccountDto> changeBalance(@PathVariable String login, @Valid @RequestBody ChangeBalanceRequest request) {

        log.info("Изменение баланса для {} на {}, key={}", login, request.amount(), request.idempotencyKey());
        return ResponseEntity.ok(accountService.changeBalance(login, request.amount(), request.idempotencyKey()));
    }

    private String extractNameFromJwt(Jwt jwt) {
        String givenName = jwt.getClaimAsString(CLAIM_GIVEN_NAME);
        String familyName = jwt.getClaimAsString(CLAIM_FAMILY_NAME);

        if (givenName != null && familyName != null) {
            return familyName + " " + givenName;
        }

        String name = jwt.getClaimAsString(CLAIM_NAME);
        if (name != null) {
            return name;
        }

        return DEFAULT_USER_NAME;
    }
}