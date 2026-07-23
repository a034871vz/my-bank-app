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
import org.springframework.test.annotation.DirtiesContext;
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
import ru.yandex.practicum.dto.TransferRequest;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Интеграционные тесты TransferController")
class TransferControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("transfer_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountsClient accountsClient;

    @MockitoBean
    private NotificationsClient notificationsClient;

    private static final String SENDER = "sender";
    private static final String RECIPIENT = "recipient";

    @BeforeEach
    void setUp() {
        reset(accountsClient, notificationsClient);
    }

    @Test
    @DisplayName("POST /transfer: должен выполнить перевод успешно")
    void transfer_Success() throws Exception {
        TransferRequest request = new TransferRequest(RECIPIENT, 500);

        when(accountsClient.changeBalance(eq(SENDER), eq(-500), anyString()))
                .thenReturn(new AccountDto(SENDER, 500));
        when(accountsClient.changeBalance(eq(RECIPIENT), eq(500), anyString()))
                .thenReturn(new AccountDto(RECIPIENT, 1500));

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderLogin", is(SENDER)))
                .andExpect(jsonPath("$.recipientLogin", is(RECIPIENT)))
                .andExpect(jsonPath("$.amount", is(500)))
                .andExpect(jsonPath("$.senderNewBalance", is(500)))
                .andExpect(jsonPath("$.message", is("Перевод 500 руб пользователю " + RECIPIENT)));

        verify(accountsClient).changeBalance(eq(SENDER), eq(-500), anyString());
        verify(accountsClient).changeBalance(eq(RECIPIENT), eq(500), anyString());
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 400 при переводе самому себе")
    void transfer_SelfTransfer_ShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest(SENDER, 500);

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Нельзя переводить самому себе")));
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 400 при недостатке средств")
    void transfer_InsufficientFunds_ShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest(RECIPIENT, 2000);

        when(accountsClient.changeBalance(eq(SENDER), eq(-2000), anyString()))
                .thenThrow(new ValidationException("Недостаточно средств"));

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Перевод не выполнен: Недостаточно средств")));
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 400 при пустом recipientLogin")
    void transfer_BlankRecipient_ShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest("", 100);

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 400 при отрицательной сумме")
    void transfer_NegativeAmount_ShouldReturnBadRequest() throws Exception {
        TransferRequest request = new TransferRequest(RECIPIENT, -100);

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 403 без роли ROLE_user")
    void transfer_WithoutRole_ShouldReturnForbidden() throws Exception {
        TransferRequest request = new TransferRequest(RECIPIENT, 100);

        mockMvc.perform(post("/transfer")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", SENDER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /transfer: должен вернуть 401 без аутентификации")
    void transfer_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        TransferRequest request = new TransferRequest(RECIPIENT, 100);

        mockMvc.perform(post("/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}