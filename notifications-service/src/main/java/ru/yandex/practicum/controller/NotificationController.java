package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.service.NotificationService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_notifications')")
    public ResponseEntity<Map<String, String>> notify(@RequestBody NotificationRequest request) {
        log.info("Получено уведомление типа {} для {}", request.type(), request.login());
        notificationService.sendNotification(request);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }
}