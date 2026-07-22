package ru.yandex.practicum.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.dto.AccountResponse;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.dto.TransferResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayClient {

    private final RestClient.Builder restClientBuilder;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${services.gateway.url}")
    private String gatewayUrl;

    private String getAccessToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName());
            if (client != null && client.getAccessToken() != null) {
                return client.getAccessToken().getTokenValue();
            }
        }
        throw new IllegalStateException("Пользователь не аутентифицирован");
    }

    private RestClient getClient() {
        return restClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AccountResponse getMyAccount() {
        log.info("Запрос данных аккаунта");
        return getClient()
                .get()
                .uri("/accounts/me")
                .retrieve()
                .body(AccountResponse.class);
    }

    public void updateAccount(String name, LocalDate birthdate) {
        log.info("Обновление аккаунта: name={}, birthdate={}", name, birthdate);
        getClient()
                .put()
                .uri("/accounts/me")
                .body(Map.of("name", name, "birthdate", birthdate.toString()))
                .retrieve()
                .body(AccountResponse.class);
    }

    public List<AccountResponse> getAllAccounts() {
        log.info("Запрос списка аккаунтов");
        return getClient()
                .get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public CashResponse processCash(String type, int amount) {
        log.info("Операция cash: type={}, amount={}", type, amount);
        return getClient()
                .post()
                .uri("/cash")
                .body(Map.of("type", type, "amount", amount))
                .retrieve()
                .body(CashResponse.class);
    }

    public TransferResponse transfer(String recipientLogin, int amount) {
        log.info("Перевод: recipient={}, amount={}", recipientLogin, amount);
        return getClient()
                .post()
                .uri("/transfer")
                .body(Map.of("recipientLogin", recipientLogin, "amount", amount))
                .retrieve()
                .body(TransferResponse.class);
    }
}