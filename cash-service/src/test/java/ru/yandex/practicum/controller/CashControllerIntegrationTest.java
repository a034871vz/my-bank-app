package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.client.AccountsClient;
import ru.yandex.practicum.client.NotificationsClient;
import ru.yandex.practicum.config.TestOAuth2Config;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.enums.CashOperationType;
import ru.yandex.practicum.exception.ValidationException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestOAuth2Config.class)
@DisplayName("Интеграционные тесты CashController")
class CashControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private NotificationsClient notificationsClient;

    private static final String LOGIN = "testuser";

    @BeforeEach
    void setUp() {
        reset(accountsClient);
    }

    @Test
    @DisplayName("POST /cash: должен выполнить DEPOSIT успешно")
    void deposit_Success() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 500);
        AccountDto accountDto = new AccountDto(LOGIN, 1500);

        when(accountsClient.changeBalance(eq(LOGIN), eq(500), anyString()))
                .thenReturn(accountDto);

        mockMvc.perform(post("/cash")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(500)))
                .andExpect(jsonPath("$.newBalance", is(1500)))
                .andExpect(jsonPath("$.message", is("Положено 500 руб")));

        verify(accountsClient).changeBalance(eq(LOGIN), eq(500), anyString());
    }

    @Test
    @DisplayName("POST /cash: должен выполнить WITHDRAW успешно")
    void withdraw_Success() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.WITHDRAW, 300);
        AccountDto accountDto = new AccountDto(LOGIN, 700);

        when(accountsClient.changeBalance(eq(LOGIN), eq(-300), anyString()))
                .thenReturn(accountDto);

        mockMvc.perform(post("/cash")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("WITHDRAW")))
                .andExpect(jsonPath("$.amount", is(300)))
                .andExpect(jsonPath("$.newBalance", is(700)))
                .andExpect(jsonPath("$.message", is("Снято 300 руб")));
    }

    @Test
    @DisplayName("POST /cash: должен вернуть 400 при отрицательной сумме")
    void deposit_WithNegativeAmount_ShouldReturnBadRequest() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, -100);

        mockMvc.perform(post("/cash")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount", is("Сумма должна быть положительной")));
    }

    @Test
    @DisplayName("POST /cash: должен вернуть 400 при недостатке средств")
    void withdraw_InsufficientFunds_ShouldReturnBadRequest() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.WITHDRAW, 1500);

        when(accountsClient.changeBalance(eq(LOGIN), eq(-1500), anyString()))
                .thenThrow(new ValidationException("Недостаточно средств на счету"));

        mockMvc.perform(post("/cash")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Операция не выполнена: Недостаточно средств на счету")));
    }

    @Test
    @DisplayName("POST /cash: должен вернуть 403 без роли ROLE_user")
    void cashOperation_WithoutRole_ShouldReturnForbidden() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 100);

        mockMvc.perform(post("/cash")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /cash: должен вернуть 401 без аутентификации")
    void cashOperation_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        CashRequest request = new CashRequest(CashOperationType.DEPOSIT, 100);

        mockMvc.perform(post("/cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}