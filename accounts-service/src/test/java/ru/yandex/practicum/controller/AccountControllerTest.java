package ru.yandex.practicum.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.config.SecurityConfig;
import ru.yandex.practicum.dto.AccountDto;
import ru.yandex.practicum.dto.AccountUpdateRequest;
import ru.yandex.practicum.exception.AccountNotFoundException;
import ru.yandex.practicum.exception.ValidationException;
import ru.yandex.practicum.service.AccountService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
@DisplayName("Юнит тесты AccountController")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    private AccountDto testAccountDto;

    @BeforeEach
    void setUp() {
        testAccountDto = new AccountDto("testuser", "Иванов Иван", "2000-05-15", 1000);
    }

    @Test
    @DisplayName("GET /accounts/me: должен вернуть аккаунт текущего пользователя")
    void getMyAccount_WhenAuthenticated_ShouldReturnAccount() throws Exception {
        doNothing().when(accountService).ensureAccountExists(any(), any(), any());
        when(accountService.getByLogin("testuser")).thenReturn(testAccountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser")
                                        .claim("given_name", "Иван")
                                        .claim("family_name", "Иванов"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", is("testuser")))
                .andExpect(jsonPath("$.name", is("Иванов Иван")))
                .andExpect(jsonPath("$.balance", is(1000)));
    }

    @Test
    @DisplayName("GET /accounts/me: должен вернуть 403 при отсутствии роли ROLE_user")
    void getMyAccount_WhenNoRoleUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts/me: должен вернуть 401 при отсутствии аутентификации")
    void getMyAccount_WhenNotAuthenticated_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /accounts/me: должен вызвать ensureAccountExists с правильными параметрами")
    void getMyAccount_ShouldCallEnsureAccountExists() throws Exception {
        doNothing().when(accountService).ensureAccountExists(any(), any(), any());
        when(accountService.getByLogin("testuser")).thenReturn(testAccountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser")
                                        .claim("name", "Иванов Иван"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk());

        verify(accountService).ensureAccountExists(eq("testuser"), eq("Иванов Иван"), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /accounts/me: должен извлечь имя из given_name и family_name")
    void getMyAccount_WithGivenAndFamilyName_ShouldExtractCorrectName() throws Exception {
        doNothing().when(accountService).ensureAccountExists(any(), any(), any());
        when(accountService.getByLogin("testuser")).thenReturn(testAccountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser")
                                        .claim("given_name", "Иван")
                                        .claim("family_name", "Иванов"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk());

        verify(accountService).ensureAccountExists(eq("testuser"), eq("Иванов Иван"), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /accounts/me: должен использовать claim 'name' при отсутствии given_name/family_name")
    void getMyAccount_WithNameClaimOnly_ShouldUseNameClaim() throws Exception {
        doNothing().when(accountService).ensureAccountExists(any(), any(), any());
        when(accountService.getByLogin("testuser")).thenReturn(testAccountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser")
                                        .claim("name", "Петров Петр"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk());

        verify(accountService).ensureAccountExists(eq("testuser"), eq("Петров Петр"), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /accounts/me: должен использовать имя по умолчанию, если нет ни одного name-claim")
    void getMyAccount_WithNoNameClaims_ShouldUseDefault() throws Exception {
        doNothing().when(accountService).ensureAccountExists(any(), any(), any());
        when(accountService.getByLogin("testuser")).thenReturn(testAccountDto);

        mockMvc.perform(get("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk());

        verify(accountService).ensureAccountExists(eq("testuser"), eq("Неизвестный пользователь"), any(LocalDate.class));
    }

    @Test
    @DisplayName("PUT /accounts/me: должен успешно обновить аккаунт")
    void updateMyAccount_WhenValidData_ShouldReturnUpdatedAccount() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Петров Петр", LocalDate.of(1995, 3, 10));
        AccountDto updated = new AccountDto("testuser", "Петров Петр", "1995-03-10", 1000);

        when(accountService.update(eq("testuser"), any(AccountUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Петров Петр")))
                .andExpect(jsonPath("$.birthdate", is("1995-03-10")));
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 400 при пустом имени")
    void updateMyAccount_WhenBlankName_ShouldReturnBadRequest() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("", LocalDate.of(1995, 3, 10));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 400 при null-дате рождения")
    void updateMyAccount_WhenNullBirthdate_ShouldReturnBadRequest() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Имя", null);

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 400 при дате рождения в будущем")
    void updateMyAccount_WhenFutureBirthdate_ShouldReturnBadRequest() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Имя", LocalDate.now().plusDays(1));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /accounts/me: должен вернуть 403 при отсутствии роли ROLE_user")
    void updateMyAccount_WhenNoRoleUser_ShouldReturnForbidden() throws Exception {
        AccountUpdateRequest request = new AccountUpdateRequest("Имя", LocalDate.of(1995, 3, 10));

        mockMvc.perform(put("/accounts/me")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /accounts: должен вернуть список всех аккаунтов")
    void getAllAccounts_ShouldReturnList() throws Exception {
        List<AccountDto> accounts = List.of(
                testAccountDto,
                new AccountDto("user2", "User Two", "1998-01-01", 500)
        );
        when(accountService.getAllForTransfer()).thenReturn(accounts);

        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].login", is("testuser")))
                .andExpect(jsonPath("$[1].login", is("user2")));
    }

    @Test
    @DisplayName("GET /accounts: должен вернуть пустой список, если аккаунтов нет")
    void getAllAccounts_WhenNoAccounts_ShouldReturnEmptyList() throws Exception {
        when(accountService.getAllForTransfer()).thenReturn(List.of());

        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(jwt -> jwt
                                        .claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /accounts: должен вернуть 403 при отсутствии роли ROLE_user")
    void getAllAccounts_WhenNoRoleUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/accounts")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен успешно изменить баланс")
    void changeBalance_WhenValidRequest_ShouldReturnUpdatedAccount() throws Exception {
        AccountDto updated = new AccountDto("testuser", "Иванов Иван", "2000-05-15", 1500);
        when(accountService.changeBalance("testuser", 500, null)).thenReturn(updated);

        mockMvc.perform(post("/accounts/testuser/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1500)));
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен передать idempotencyKey в сервис")
    void changeBalance_WhenIdempotencyKey_ShouldPassToService() throws Exception {
        AccountDto updated = new AccountDto("testuser", "Иванов Иван", "2000-05-15", 1200);
        when(accountService.changeBalance("testuser", 200, "key-123")).thenReturn(updated);

        mockMvc.perform(post("/accounts/testuser/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 200,
                                "idempotencyKey", "key-123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(1200)));
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен использовать amount=0 по умолчанию")
    void changeBalance_WhenAmountMissing_ShouldDefaultToZero() throws Exception {
        AccountDto updated = new AccountDto("testuser", "Иванов Иван", "2000-05-15", 1000);
        when(accountService.changeBalance("testuser", 0, null)).thenReturn(updated);

        mockMvc.perform(post("/accounts/testuser/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен вернуть 403 при отсутствии роли ROLE_accounts")
    void changeBalance_WhenNoRoleAccounts_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/accounts/testuser/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 500))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен пробросить ValidationException")
    void changeBalance_WhenInsufficientFunds_ShouldReturnBadRequest() throws Exception {
        when(accountService.changeBalance("testuser", -1500, null))
                .thenThrow(new ValidationException("Недостаточно средств на счету"));

        mockMvc.perform(post("/accounts/testuser/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", -1500))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /accounts/{login}/balance: должен пробросить AccountNotFoundException")
    void changeBalance_WhenAccountNotFound_ShouldReturnNotFound() throws Exception {
        when(accountService.changeBalance("unknown", 100, null))
                .thenThrow(new AccountNotFoundException("unknown"));

        mockMvc.perform(post("/accounts/unknown/balance")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "testuser"))
                                .authorities(new SimpleGrantedAuthority("ROLE_accounts")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("amount", 100))))
                .andExpect(status().isNotFound());
    }
}