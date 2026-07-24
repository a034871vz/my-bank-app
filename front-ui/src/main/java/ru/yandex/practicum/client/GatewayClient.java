package ru.yandex.practicum.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class GatewayClient {

    private final RestClient restClient;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public GatewayClient(RestClient.Builder restClientBuilder, OAuth2AuthorizedClientService authorizedClientService,
            @Value("${services.gateway.url}") String gatewayUrl) {
        this.authorizedClientService = authorizedClientService;
        this.restClient = restClientBuilder
                .baseUrl(gatewayUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultStatusHandler(
                        HttpStatusCode::isError,
                        (request, response) -> {
                            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            log.error("Ошибка сервера Gateway [{}] статус: {}, тело: {}",
                                    request.getURI(), response.getStatusCode(), body);
                            throw new IllegalArgumentException(body.isBlank() ? "Ошибка сервера Gateway" : body);
                        }
                )
                .build();
    }

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

    private Consumer<HttpHeaders> authHeader() {
        return headers -> headers.setBearerAuth(getAccessToken());
    }

    public AccountResponse getMyAccount() {
        log.info("Запрос данных аккаунта");
        return restClient
                .get()
                .uri("/accounts/me")
                .headers(authHeader()) // Токен проставляется динамически при каждом запросе
                .retrieve()
                .body(AccountResponse.class);
    }

    public void updateAccount(String name, LocalDate birthdate) {
        log.info("Обновление аккаунта: name={}, birthdate={}", name, birthdate);
        restClient
                .put()
                .uri("/accounts/me")
                .headers(authHeader())
                .body(Map.of("name", name, "birthdate", birthdate.toString()))
                .retrieve()
                .body(AccountResponse.class);
    }

    public List<AccountResponse> getAllAccounts() {
        log.info("Запрос списка аккаунтов");
        return restClient
                .get()
                .uri("/accounts")
                .headers(authHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public CashResponse processCash(String type, int amount) {
        log.info("Операция cash: type={}, amount={}", type, amount);
        return restClient
                .post()
                .uri("/cash")
                .headers(authHeader())
                .body(Map.of("type", type, "amount", amount))
                .retrieve()
                .body(CashResponse.class);
    }

    public TransferResponse transfer(String recipientLogin, int amount) {
        log.info("Перевод: recipient={}, amount={}", recipientLogin, amount);
        return restClient
                .post()
                .uri("/transfer")
                .headers(authHeader())
                .body(Map.of("recipientLogin", recipientLogin, "amount", amount))
                .retrieve()
                .body(TransferResponse.class);
    }
}