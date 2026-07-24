package ru.yandex.practicum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.NotificationsClient;
import ru.yandex.practicum.dto.NotificationEventPayload;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.entity.OutboxEvent;
import ru.yandex.practicum.repository.OutboxEventRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationsClient notificationsClient;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<OutboxEvent> events = outboxEventRepository.findUnprocessed();

        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                outboxService.markAsProcessed(event.getId());
                log.info("Outbox событие #{} обработано", event.getId());

            } catch (Exception e) {
                int nextRetry = event.getRetryCount() + 1;
                if (nextRetry >= MAX_RETRIES) {
                    outboxService.moveToDeadLetter(event.getId(), "Максимум попыток (" + MAX_RETRIES + ") достигнуто. Ошибка : " + e.getMessage());
                } else {
                    outboxService.registerError(event.getId(), nextRetry, e.getMessage());
                }
                log.error("Outbox событие #{} ошибка (попытка {}/{}): {}",
                        event.getId(), nextRetry, MAX_RETRIES, e.getMessage());
            }
        }
    }

    private void processEvent(OutboxEvent event) {
        log.info("Обработка события {} типа {} для {}", event.getId(), event.getEventType(), event.getDestination());

        if ("notifications-service".equals(event.getDestination())) {
            processNotification(event);
        } else {
            throw new IllegalArgumentException("Неизвестный destination: " + event.getDestination());
        }
    }

    private void processNotification(OutboxEvent event) {
        try {
            NotificationEventPayload payload = objectMapper.readValue(event.getPayload(), NotificationEventPayload.class);

            NotificationRequest request = new NotificationRequest(
                    payload.type(),
                    payload.login(),
                    payload.amount(),
                    payload.message()
            );

            notificationsClient.sendNotification(request, payload.sagaId());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга или отправки уведомления: " + e.getMessage(), e);
        }
    }
}