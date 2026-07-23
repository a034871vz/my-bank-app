package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.dto.AccountUpdateRequest;
import ru.yandex.practicum.entity.Account;
import ru.yandex.practicum.repository.AccountRepository;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Интеграционные тесты AccountController")
class AccountControllerIntegrationTest {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;
    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_NAME = "Иванов Иван";
    private static final LocalDate TEST_BIRTHDATE = LocalDate.of(2000, 5, 15);

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        testAccount = accountRepository.save(
                new Account(TEST_LOGIN, TEST_NAME, TEST_BIRTHDATE, 1000)
        );
    }

    @Test
    @DisplayName("GET /accounts/me: должен вернуть аккаунт текущего пользователя")
    void getMyAccount_ShouldReturnAccount() throws Exception {
        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", TEST_LOGIN)
                                        .claim("given_name", "Иван")
                                        .claim("family_name", "Иванов"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is(TEST_LOGIN)))
                .andExpect(jsonPath("$.name", is(TEST_NAME)))
                .andExpect(jsonPath("$.balance", is(1000)));
    }

    @Test
    @DisplayName("GET /accounts/me: должен создать пользователя, если его нет")
    void getMyAccount_WhenUserNotExists_ShouldCreateAndReturn() throws Exception {
        String newLogin = "newuser";
        String newName = "Новый Пользователь";

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", newLogin)
                                        .claim("name", newName))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is(newLogin)))
                .andExpect(jsonPath("$.name", is(newName)))
                .andExpect(jsonPath("$.balance", is(0)));

        Account saved = accountRepository.findByLogin(newLogin).orElseThrow();
        assertThat(saved.getName()).isEqualTo(newName);
        assertThat(saved.getBalance()).isZero();
    }

    @Test
    @DisplayName("GET /accounts/me: должен вернуть 403 без роли ROLE_user")
    void getMyAccount_WithoutRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", TEST_LOGIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /accounts/me: должен обновить данные аккаунта")
    void updateMyAccount_ShouldUpdate() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Петров Петр", LocalDate.of(1995, 3, 10));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", TEST_LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Петров Петр")))
                .andExpect(jsonPath("$.birthdate", is("1995-03-10")))
                .andExpect(jsonPath("$.balance", is(1000)));

        Account updated = accountRepository.findByLogin(TEST_LOGIN).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Петров Петр");
        assertThat(updated.getBirthdate()).isEqualTo(LocalDate.of(1995, 3, 10));
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 400 при возрасте < 18")
    void updateMyAccount_WhenUnder18_ShouldReturnBadRequest() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Молодой", LocalDate.now().minusYears(17));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", TEST_LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Возраст должен быть старше 18 лет")));
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 403 без роли ROLE_user")
    void updateMyAccount_WithoutRole_ShouldReturnForbidden() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Имя", LocalDate.of(1995, 3, 10));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", TEST_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("GET /accounts: должен вернуть список всех аккаунтов")
    void getAllAccounts_ShouldReturnList() throws Exception {
        Account other = new Account("user2", "User Two", LocalDate.of(1998, 1, 1), 500);
        accountRepository.save(other);

        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", TEST_LOGIN))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.login=='testuser')].name", hasItem("Иванов Иван")))
                .andExpect(jsonPath("$[?(@.login=='user2')].name", hasItem("User Two")));
    }

    @Test
    @DisplayName("GET /accounts: должен вернуть 403 без роли ROLE_user")
    void getAllAccounts_WithoutRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", TEST_LOGIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен изменить баланс (пополнение)")
    void changeBalance_Deposit_ShouldIncrease() throws Exception {
        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1500)));

        Account updated = accountRepository.findByLogin(TEST_LOGIN).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(1500);
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен изменить баланс (списание)")
    void changeBalance_Withdraw_ShouldDecrease() throws Exception {
        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", -300))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(700)));
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен вернуть 400 при недостатке средств")
    void changeBalance_InsufficientFunds_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", -1500))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Недостаточно средств")));
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен обработать idempotencyKey")
    void changeBalance_WithIdempotencyKey_ShouldReturnCachedResult() throws Exception {
        String key = "unique-key-123";

        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 200,
                                "idempotencyKey", key
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1200)));

        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 500,
                                "idempotencyKey", key
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1200)));

        Account updated = accountRepository.findByLogin(TEST_LOGIN).orElseThrow();
        assertThat(updated.getBalance()).isEqualTo(1200);
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен вернуть 403 без роли ROLE_accounts")
    void changeBalance_WithoutRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/accounts/" + TEST_LOGIN + "/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "service")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 100))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен вернуть 404 при отсутствии пользователя")
    void changeBalance_UserNotFound_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/accounts/unknown/balance")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 100))))
                .andExpect(status().isNotFound());
    }
}