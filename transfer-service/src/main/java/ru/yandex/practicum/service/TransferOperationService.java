package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.entity.TransferOperation;
import ru.yandex.practicum.enums.NotificationType;
import ru.yandex.practicum.repository.TransferOperationRepository;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferOperationService {

    private final TransferOperationRepository transferOperationRepository;
    private final OutboxService outboxService;

    @Transactional
    public void saveOperation(String senderLogin, TransferRequest request, int senderNewBalance, String sagaId) {
        log.info("Сохранение перевода {}→{} на {} руб, saga={}",
                senderLogin, request.recipientLogin(), request.amount(), sagaId);

        transferOperationRepository.save(new TransferOperation(
                senderLogin, request.recipientLogin(), request.amount()));

        Map<String, Object> senderPayload = Map.of(
                "type", NotificationType.TRANSFER.name(),
                "login", senderLogin,
                "amount", request.amount(),
                "message", "Вы перевели " + request.amount() + " руб пользователю " + request.recipientLogin(),
                "newBalance", senderNewBalance,
                "sagaId", sagaId
        );
        outboxService.publish("TRANSFER_SENT", senderPayload, "notifications-service", sagaId);

        Map<String, Object> recipientPayload = Map.of(
                "type", NotificationType.TRANSFER.name(),
                "login", request.recipientLogin(),
                "amount", request.amount(),
                "message", "Вам перевели " + request.amount() + " руб от пользователя " + senderLogin,
                "sagaId", sagaId + "-recv"
        );
        outboxService.publish("TRANSFER_RECEIVED", recipientPayload, "notifications-service", sagaId + "-recv");
    }
}