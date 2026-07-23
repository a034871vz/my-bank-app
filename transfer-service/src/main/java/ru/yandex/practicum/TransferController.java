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
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.dto.TransferResponse;
import ru.yandex.practicum.service.TransferService;

@Slf4j
@RestController
@RequestMapping("/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_user')")
    public ResponseEntity<TransferResponse> transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        String senderLogin = jwt.getClaimAsString("preferred_username");
        log.info("Пользователь {} запросил перевод {} руб пользователю {}",
                senderLogin, request.amount(), request.recipientLogin());

        return ResponseEntity.ok(transferService.process(senderLogin, request));
    }
}