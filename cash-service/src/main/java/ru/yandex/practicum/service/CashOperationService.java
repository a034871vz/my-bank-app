package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.dto.NotificationEventPayload;
import ru.yandex.practicum.entity.CashOperation;
import ru.yandex.practicum.enums.NotificationType;
import ru.yandex.practicum.repository.CashOperationRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashOperationService {

    private final CashOperationRepository cashOperationRepository;
    private final OutboxService outboxService;

    @Transactional
    public void saveOperation(String login, CashRequest request, int newBalance, String sagaId) {
        log.info("Сохранение операции {} для {}, saga={}", request.type(), login, sagaId);

        cashOperationRepository.save(new CashOperation(login, request));

        String message = switch (request.type()) {
            case DEPOSIT -> "Положено " + request.amount() + " руб";
            case WITHDRAW -> "Снято " + request.amount() + " руб";
        };

        NotificationType notificationType = switch (request.type()) {
            case DEPOSIT -> NotificationType.DEPOSIT;
            case WITHDRAW -> NotificationType.WITHDRAW;
        };

        NotificationEventPayload payload = new NotificationEventPayload(notificationType, login,
                request.amount(), message, newBalance, sagaId);

        String eventType = switch (request.type()) {
            case DEPOSIT -> "CASH_DEPOSIT";
            case WITHDRAW -> "CASH_WITHDRAW";
        };

        outboxService.publish(eventType, payload, "notifications-service", sagaId);
    }
}