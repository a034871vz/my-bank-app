package ru.yandex.practicum.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.NotificationRequest;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotificationService {

    private final Cache<String, Boolean> processedKeys = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build();

    public boolean sendNotification(NotificationRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (processedKeys.getIfPresent(idempotencyKey) != null) {
                log.info("Дубль уведомления с key={}", idempotencyKey);
                return true;
            }
        }

        switch (request.type()) {
            case DEPOSIT -> log.info("Уведомление: пользователь {} пополнил счёт на {} руб. — {}",
                    request.login(), request.amount(), request.message());
            case WITHDRAW -> log.info("Уведомление: пользователь {} снял со счёта {} руб. — {}",
                    request.login(), request.amount(), request.message());
            case TRANSFER -> log.info("Уведомление: перевод {} руб. — {}",
                    request.amount(), request.message());
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            processedKeys.put(idempotencyKey, Boolean.TRUE);
        }

        return false;
    }
}