package ru.yandex.practicum.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.config.TestSecurityConfig;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.repository.OutboxEventRepository;
import ru.yandex.practicum.service.OutboxPoller;
import ru.yandex.practicum.service.OutboxService;
import ru.yandex.practicum.service.SagaCompensator;
import ru.yandex.practicum.service.TransferOperationService;
import ru.yandex.practicum.service.TransferService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.consul.enabled=false",
        "spring.cloud.consul.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "accounts-service.url=http://localhost:8080",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
@AutoConfigureStubRunner(
        ids = "ru.yandex.practicum:accounts-service:+:stubs:8080",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(TestSecurityConfig.class)
class AccountsClientContractTest {

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private TransferOperationService transferOperationService;

    @MockitoBean
    private OutboxService outboxService;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private SagaCompensator sagaCompensator;

    @MockitoBean
    private OutboxPoller outboxPoller;

    @MockitoBean
    private OAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    private AccountsClient accountsClient;

    @BeforeEach
    void setUp() {
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "dummy-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        ClientRegistration registration = ClientRegistration
                .withRegistrationId("transfer-service")
                .tokenUri("http://localhost:8080/dummy-token")
                .clientId("dummy-client")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(registration, "transfer-service", token);

        when(authorizedClientManager.authorize(any())).thenReturn(client);
    }

    @Test
    void shouldChangeBalanceSuccessfully() {
        AccountDto result = accountsClient.changeBalance("testuser", 500, "key-123");
        assertThat(result.login()).isEqualTo("testuser");
        assertThat(result.balance()).isEqualTo(1500);
    }

    @Test
    void shouldRejectInsufficientFunds() {
        assertThatThrownBy(() -> accountsClient.changeBalance("testuser", -1500, "key-456"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Недостаточно средств");
    }
}