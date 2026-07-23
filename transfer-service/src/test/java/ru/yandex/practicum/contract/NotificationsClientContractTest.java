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
import ru.yandex.practicum.client.NotificationsClient;
import ru.yandex.practicum.config.TestSecurityConfig;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.enums.NotificationType;
import ru.yandex.practicum.repository.OutboxEventRepository;
import ru.yandex.practicum.service.OutboxPoller;
import ru.yandex.practicum.service.OutboxService;
import ru.yandex.practicum.service.SagaCompensator;
import ru.yandex.practicum.service.TransferOperationService;
import ru.yandex.practicum.service.TransferService;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.consul.enabled=false",
        "spring.cloud.consul.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "notifications-service.url=http://localhost:8081",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
@AutoConfigureStubRunner(
        ids = "ru.yandex.practicum:notifications-service:+:stubs:8081",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
@Import(TestSecurityConfig.class)
class NotificationsClientContractTest {

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
    private NotificationsClient notificationsClient;

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
                .tokenUri("http://localhost:8081/dummy-token")
                .clientId("dummy-client")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(registration, "transfer-service", token);

        when(authorizedClientManager.authorize(any())).thenReturn(client);
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT,
                "testuser",
                500,
                "Пополнение счета"
        );

        assertThatNoException().isThrownBy(() ->
                notificationsClient.sendNotification(request, "notif-key-123")
        );
    }
}