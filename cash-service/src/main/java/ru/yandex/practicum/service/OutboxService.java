package ru.yandex.practicum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.OutboxEvent;
import ru.yandex.practicum.repository.OutboxEventRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String eventType, Object payload, String destination, String sagaId) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .payload(toJson(payload))
                .destination(destination)
                .sagaId(sagaId)
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventRepository.save(event);
        log.info("Событие опубликовано в outbox: type={}, saga={}", eventType, sagaId);
    }

    @SneakyThrows
    private String toJson(Object payload) {
        return objectMapper.writeValueAsString(payload);
    }

    @Transactional
    public void markAsProcessed(Long eventId) {
        outboxEventRepository.findById(eventId)
                .ifPresent(event -> event.setProcessedAt(LocalDateTime.now()));
    }

    @Transactional
    public void registerError(Long eventId, int newRetryCount, String errorMessage) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setRetryCount(newRetryCount);
            event.setError(errorMessage);
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void moveToDeadLetter(Long eventId, String reason) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            log.error("Событие #{} перемещено в dead letter: {}", eventId, reason);
            event.setFailedAt(LocalDateTime.now());
            event.setError(reason);
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        });
    }
}