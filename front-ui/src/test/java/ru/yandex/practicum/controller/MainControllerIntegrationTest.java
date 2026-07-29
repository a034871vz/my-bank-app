package ru.yandex.practicum.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.client.GatewayClient;
import ru.yandex.practicum.config.TestOAuth2Config;
import ru.yandex.practicum.dto.AccountResponse;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.dto.TransferResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestOAuth2Config.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Интеграционные тесты MainController")
class MainControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GatewayClient gatewayClient;

    private static final String TEST_LOGIN = "testuser";
    private static final String TEST_NAME = "Иванов Иван";

    @BeforeEach
    void setUp() {
        reset(gatewayClient);
    }

    private Consumer<Map<String, Object>> defaultAttrs() {
        return attrs -> {
            attrs.put("sub", TEST_LOGIN);
            attrs.put("preferred_username", TEST_LOGIN);
            attrs.put("given_name", "Иван");
            attrs.put("family_name", "Иванов");
            attrs.put("name", TEST_NAME);
        };
    }

    @Test
    @DisplayName("GET /account: должен вернуть страницу аккаунта")
    void getAccount_ShouldReturnMainPage() throws Exception {
        AccountResponse account = new AccountResponse(TEST_LOGIN, TEST_NAME, "2000-05-15", 1000);
        List<AccountResponse> accounts = List.of(
                account,
                new AccountResponse("user2", "Петров Петр", "1995-03-10", 500)
        );

        when(gatewayClient.getMyAccount()).thenReturn(account);
        when(gatewayClient.getAllAccounts()).thenReturn(accounts);

        mockMvc.perform(get("/account")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs())
                                .authorities(new SimpleGrantedAuthority("ROLE_user"))))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("name", TEST_NAME))
                .andExpect(model().attribute("sum", 1000))
                .andExpect(model().attribute("accounts", accounts));

        verify(gatewayClient).getMyAccount();
        verify(gatewayClient).getAllAccounts();
    }

    @Test
    @DisplayName("GET /account: должен обработать ошибку GatewayClient")
    void getAccount_WhenGatewayError_ShouldShowError() throws Exception {
        when(gatewayClient.getMyAccount())
                .thenThrow(new RuntimeException("Сервис недоступен"));

        mockMvc.perform(get("/account")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs())))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    @DisplayName("POST /account: должен обновить данные")
    void editAccount_ShouldUpdate() throws Exception {
        AccountResponse updated = new AccountResponse(TEST_LOGIN, "Петров Петр", "1995-03-10", 1000);
        List<AccountResponse> accounts = List.of(updated);

        when(gatewayClient.getMyAccount()).thenReturn(updated);
        when(gatewayClient.getAllAccounts()).thenReturn(accounts);

        mockMvc.perform(post("/account")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs()))
                        .param("name", "Петров Петр")
                        .param("birthdate", "1995-03-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Данные сохранены"));

        verify(gatewayClient).updateAccount("Петров Петр", java.time.LocalDate.of(1995, 3, 10));
    }

    @Test
    @DisplayName("POST /cash DEPOSIT: должен пополнить баланс")
    void cashDeposit_ShouldIncreaseBalance() throws Exception {
        CashResponse cashResponse = new CashResponse(TEST_LOGIN, "DEPOSIT", 500, 1500, "Положено 500 руб");
        AccountResponse account = new AccountResponse(TEST_LOGIN, TEST_NAME, "2000-05-15", 1500);
        List<AccountResponse> accounts = List.of(account);

        when(gatewayClient.processCash("DEPOSIT", 500)).thenReturn(cashResponse);
        when(gatewayClient.getMyAccount()).thenReturn(account);
        when(gatewayClient.getAllAccounts()).thenReturn(accounts);

        mockMvc.perform(post("/cash")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs()))
                        .param("value", "500")
                        .param("action", "PUT"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Положено 500 руб"))
                .andExpect(model().attribute("sum", 1500));

        verify(gatewayClient).processCash("DEPOSIT", 500);
    }

    @Test
    @DisplayName("POST /cash WITHDRAW: должен списать средства")
    void cashWithdraw_ShouldDecreaseBalance() throws Exception {
        CashResponse cashResponse = new CashResponse(TEST_LOGIN, "WITHDRAW", 300, 700, "Снято 300 руб");
        AccountResponse account = new AccountResponse(TEST_LOGIN, TEST_NAME, "2000-05-15", 700);
        List<AccountResponse> accounts = List.of(account);

        when(gatewayClient.processCash("WITHDRAW", 300)).thenReturn(cashResponse);
        when(gatewayClient.getMyAccount()).thenReturn(account);
        when(gatewayClient.getAllAccounts()).thenReturn(accounts);

        mockMvc.perform(post("/cash")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs()))
                        .param("value", "300")
                        .param("action", "GET"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Снято 300 руб"));

        verify(gatewayClient).processCash("WITHDRAW", 300);
    }

    @Test
    @DisplayName("POST /cash: должен показать ошибку при недостатке средств")
    void cashWithdraw_WhenInsufficientFunds_ShouldShowError() throws Exception {
        when(gatewayClient.processCash("WITHDRAW", 2000))
                .thenThrow(new RuntimeException("Недостаточно средств"));

        AccountResponse account = new AccountResponse(TEST_LOGIN, TEST_NAME, "2000-05-15", 1000);
        when(gatewayClient.getMyAccount()).thenReturn(account);
        when(gatewayClient.getAllAccounts()).thenReturn(List.of(account));

        mockMvc.perform(post("/cash")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs()))
                        .param("value", "2000")
                        .param("action", "GET"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    @DisplayName("POST /transfer: должен выполнить перевод")
    void transfer_ShouldTransferFunds() throws Exception {
        TransferResponse transferResponse = new TransferResponse(
                TEST_LOGIN, "recipient", 500, 500, "Перевод выполнен");
        AccountResponse account = new AccountResponse(TEST_LOGIN, TEST_NAME, "2000-05-15", 500);
        List<AccountResponse> accounts = List.of(account);

        when(gatewayClient.transfer("recipient", 500)).thenReturn(transferResponse);
        when(gatewayClient.getMyAccount()).thenReturn(account);
        when(gatewayClient.getAllAccounts()).thenReturn(accounts);

        mockMvc.perform(post("/transfer")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs()))
                        .param("value", "500")
                        .param("login", "recipient"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("info", "Перевод выполнен"));

        verify(gatewayClient).transfer("recipient", 500);
    }

    @Test
    @DisplayName("GET /: аноним редиректит на OAuth2 login")
    void index_WhenAnonymous_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/gateway"));
    }

    @Test
    @DisplayName("GET /: аутентифицированный редиректит на /account")
    void index_WhenAuthenticated_ShouldRedirectToAccount() throws Exception {
        mockMvc.perform(get("/")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration())
                                .attributes(defaultAttrs())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/account"));
    }

    @Test
    @DisplayName("GET /account: должен требовать аутентификацию")
    void getAccount_WithoutAuth_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection());
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("gateway")
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
                .build();
    }
}