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
import ru.yandex.practicum.service.AccountService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<AccountDto> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getClaimAsString("preferred_username");
        String name = extractNameFromJwt(jwt);

        accountService.ensureAccountExists(login, name, LocalDate.of(2001, 1, 1));

        log.info("Пользователь {} запросил данные аккаунта", login);
        return ResponseEntity.ok(accountService.getByLogin(login));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<AccountDto> updateMyAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AccountUpdateRequest request) {
        String login = jwt.getClaimAsString("preferred_username");
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
    public ResponseEntity<AccountDto> changeBalance(@PathVariable String login, @RequestBody Map<String, Object> body) {

        int amount = (Integer) body.getOrDefault("amount", 0);
        String idempotencyKey = (String) body.get("idempotencyKey");

        log.info("Изменение баланса для {} на {}, key={}", login, amount, idempotencyKey);
        return ResponseEntity.ok(accountService.changeBalance(login, amount, idempotencyKey));
    }

    private String extractNameFromJwt(Jwt jwt) {
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");

        if (givenName != null && familyName != null) {
            return familyName + " " + givenName;
        }

        String name = jwt.getClaimAsString("name");
        if (name != null) {
            return name;
        }

        return "Неизвестный пользователь";
    }
}