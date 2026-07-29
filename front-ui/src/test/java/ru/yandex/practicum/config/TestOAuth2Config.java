package ru.yandex.practicum.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@TestConfiguration
public class TestOAuth2Config {

    @Bean
    @Primary
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId("gateway")
                        .clientId("test-client")
                        .clientSecret("test-secret")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://localhost:8080/login/oauth2/code/gateway")
                        .scope("openid", "profile")
                        .authorizationUri("http://localhost:8082/realms/bank-realm/protocol/openid-connect/auth")
                        .tokenUri("http://localhost:8082/realms/bank-realm/protocol/openid-connect/token")
                        .userInfoUri("http://localhost:8082/realms/bank-realm/protocol/openid-connect/userinfo")
                        .userNameAttributeName("preferred_username")
                        .clientName("Gateway")
                        .build()
        );
    }
}