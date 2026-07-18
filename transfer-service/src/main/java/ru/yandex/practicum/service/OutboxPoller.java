package ru.yandex.practicum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.NotificationsClient;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.entity.OutboxEvent;
import ru.yandex.practicum.enums.NotificationType;
import ru.yandex.practicum.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private static final int MAX_RETRIES = 3;

    private final OutboxEventRepository outboxEventRepository;
    private final NotificationsClient notificationsClient;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void poll() {
        var events = outboxEventRepository.findUnprocessed();

        for (OutboxEvent event : events) {
            if (event.getRetryCount() >= MAX_RETRIES) {
                moveToDeadLetter(event, "Max retries exceeded");
                continue;
            }

            try {
                processEvent(event);
                event.setProcessedAt(LocalDateTime.now());
                log.info("Outbox событие #{} обработано", event.getId());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setError(e.getMessage());
                log.error("Outbox событие #{} ошибка (попытка {}/{}): {}",
                        event.getId(), event.getRetryCount(), MAX_RETRIES, e.getMessage());
            }

            outboxEventRepository.save(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        log.info("Обработка события {} типа {} для {}", event.getId(), event.getEventType(), event.getDestination());

        if (event.getDestination().equals("notifications-service")) {
            processNotification(event);
        } else {
            throw new IllegalArgumentException("Unknown destination: " + event.getDestination());
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private void processNotification(OutboxEvent event) {
        Map<String, Object> payload = objectMapper.readValue(event.getPayload(), Map.class);

        NotificationRequest request = new NotificationRequest(
                NotificationType.valueOf((String) payload.get("type")),
                (String) payload.get("login"),
                (Integer) payload.get("amount"),
                (String) payload.get("message")
        );

        String idempotencyKey = (String) payload.get("sagaId");
        notificationsClient.sendNotification(request, idempotencyKey);
    }

    private void moveToDeadLetter(OutboxEvent event, String reason) {
        log.error("Событие #{} перемещено в dead letter: {}", event.getId(), reason);
        event.setFailedAt(LocalDateTime.now());
        event.setError(reason);
        event.setProcessedAt(LocalDateTime.now());
    }
}