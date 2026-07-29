package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.NotificationEventPayload;
import ru.yandex.practicum.dto.TransferRequest;
import ru.yandex.practicum.entity.TransferOperation;
import ru.yandex.practicum.enums.NotificationType;
import ru.yandex.practicum.repository.TransferOperationRepository;

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

        NotificationEventPayload senderPayload = new NotificationEventPayload(
                NotificationType.TRANSFER,
                senderLogin,
                request.amount(),
                "Вы перевели " + request.amount() + " руб пользователю " + request.recipientLogin(),
                senderNewBalance,
                sagaId
        );
        outboxService.publish("TRANSFER_SENT", senderPayload, "notifications-service", sagaId);

        NotificationEventPayload recipientPayload = new NotificationEventPayload(
                NotificationType.TRANSFER,
                request.recipientLogin(),
                request.amount(),
                "Вам перевели " + request.amount() + " руб от пользователя " + senderLogin,
                null,
                sagaId + "-recv"
        );
        outboxService.publish("TRANSFER_RECEIVED", recipientPayload, "notifications-service", sagaId + "-recv");
    }
}