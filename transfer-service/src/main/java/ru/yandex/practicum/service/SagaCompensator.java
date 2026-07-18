package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.client.AccountsClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCompensator {

    private final AccountsClient accountsClient;

    public void compensate(String login, int reverseAmount, String compensationKey) {
        log.warn("Компенсация баланса для {} на {}, key={}", login, reverseAmount, compensationKey);
        try {
            accountsClient.changeBalance(login, reverseAmount, compensationKey);
            log.info("Компенсация для {} успешна", login);
        } catch (Exception e) {
            log.error("КРИТИЧНО: Компенсация не удалась! login={}, amount={}, error={}",
                    login, reverseAmount, e.getMessage());
        }
    }
}