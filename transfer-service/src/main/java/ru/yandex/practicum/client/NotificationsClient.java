package ru.yandex.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.dto.NotificationRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationsClient {

    private final RestClient.Builder restClientBuilder;

    public void sendNotification(NotificationRequest request, String idempotencyKey) {
        log.info("Отправка уведомления типа {} для {}, key={}", request.type(), request.login(), idempotencyKey);

        var requestSpec = restClientBuilder.build()
                .post()
                .uri("http://notifications-service/notifications")
                .header("Content-Type", "application/json");

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestSpec.header("X-Idempotency-Key", idempotencyKey);
        }

        requestSpec
                .body(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (req, response) -> {
                            String body = new String(response.getBody().readAllBytes());
                            log.error("Notifications error: {} - {}", response.getStatusCode(), body);
                            throw new RuntimeException("Notifications error: " + body);
                        }
                )
                .toBodilessEntity();

        log.info("Уведомление для {} отправлено", request.login());
    }
}