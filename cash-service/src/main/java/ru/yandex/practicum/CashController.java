package ru.yandex.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.service.CashService;

@Slf4j
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<CashResponse> process(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CashRequest request) {
        String login = jwt.getClaimAsString("preferred_username");
        log.info("Пользователь {} запросил операцию {}", login, request.type());
        return ResponseEntity.ok(cashService.process(login, request));
    }
}