package ru.yandex.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.NotificationRequest;

@Slf4j
@Service
public class NotificationService {

    public void sendNotification(NotificationRequest request) {
        switch (request.type()) {
            case "DEPOSIT" -> log.info("Уведомление: пользователь {} пополнил счёт на {} руб. — {}",
                    request.login(), request.amount(), request.message());
            case "WITHDRAW" -> log.info("Уведомление: пользователь {} снял со счёта {} руб. — {}",
                    request.login(), request.amount(), request.message());
            case "TRANSFER" -> log.info("Уведомление: перевод {} руб. — {}",
                    request.amount(), request.message());
            default -> log.info("Уведомление для {}: {}", request.login(), request.message());
        }
    }
}