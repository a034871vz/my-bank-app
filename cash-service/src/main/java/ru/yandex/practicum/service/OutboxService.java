package ru.yandex.practicum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.OutboxEvent;
import ru.yandex.practicum.enums.OutboxStatus; // Если используешь Enum для статусов
import ru.yandex.practicum.repository.OutboxEventRepository;

import java.time.LocalDateTime;
import java.util.List;

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
                .status(OutboxStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        outboxEventRepository.save(event);
        log.info("Событие опубликовано в outbox: type={}, saga={}", eventType, sagaId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> lockAndFetchBatch(int batchSize) {
        List<OutboxEvent> events = outboxEventRepository.findUnprocessedForUpdate(PageRequest.of(0, batchSize));

        for (OutboxEvent event : events) {
            event.setStatus(OutboxStatus.PROCESSING);
        }

        return events;
    }

    @Transactional
    public void markAsProcessed(Long eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setProcessedAt(LocalDateTime.now());
        });
    }

    @Transactional
    public void registerError(Long eventId, int newRetryCount, String errorMessage) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setRetryCount(newRetryCount);
            event.setError(errorMessage);
            event.setStatus(OutboxStatus.NEW);
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void moveToDeadLetter(Long eventId, String reason) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            log.error("Событие #{} перемещено в dead letter: {}", eventId, reason);
            event.setStatus(OutboxStatus.FAILED);
            event.setFailedAt(LocalDateTime.now());
            event.setError(reason);
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        });
    }

    @SneakyThrows
    private String toJson(Object payload) {
        return objectMapper.writeValueAsString(payload);
    }
}