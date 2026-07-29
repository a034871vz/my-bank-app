package ru.yandex.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient.Builder restClientBuilder;

    public AccountDto changeBalance(String login, int amount, String idempotencyKey) {
        log.info("Accounts: изменение баланса для {} на {}, key={}", login, amount, idempotencyKey);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("idempotencyKey", idempotencyKey);

        return restClientBuilder.build()
                .post()
                .uri("http://accounts-service/accounts/{login}/balance", login)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                            String respBody = new String(response.getBody().readAllBytes());
                            log.error("Accounts error: {} - {}", response.getStatusCode(), respBody);
                            throw new ValidationException("Accounts error: " + respBody);
                        }
                )
                .body(AccountDto.class);
    }
}