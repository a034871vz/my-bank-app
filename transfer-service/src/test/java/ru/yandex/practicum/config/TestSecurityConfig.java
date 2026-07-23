package ru.yandex.practicum.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("transfer-service")
                .tokenUri("http://localhost:8080/dummy-token")
                .clientId("dummy-client")
                .clientSecret("dummy-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @Primary
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository repository) {
        return mock(OAuth2AuthorizedClientService.class);
    }

    @Bean
    @Primary
    public OAuth2AuthorizedClientManager authorizedClientManager() {
        return mock(OAuth2AuthorizedClientManager.class);
    }
}