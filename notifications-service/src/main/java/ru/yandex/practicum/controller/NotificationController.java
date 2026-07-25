package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.dto.NotificationResponse;
import ru.yandex.practicum.service.NotificationService;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final String STATUS_ALREADY_PROCESSED = "already-processed";
    private static final String STATUS_SENT = "sent";

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_notifications')")
    public ResponseEntity<NotificationResponse> notify(@Valid @RequestBody NotificationRequest request,
                                                       @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        log.info("Получено уведомление типа {} для {}, key={}", request.type(), request.login(), idempotencyKey);

        boolean alreadyProcessed = notificationService.sendNotification(request, idempotencyKey);

        String status = alreadyProcessed ? STATUS_ALREADY_PROCESSED : STATUS_SENT;
        return ResponseEntity.ok(new NotificationResponse(status));
    }
}