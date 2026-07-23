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
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.enums.NotificationType;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Интеграционные тесты NotificationController")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /notifications: должен отправить уведомление")
    void notify_ShouldReturnSent() throws Exception {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT, "testuser", 500, "Пополнение");

        mockMvc.perform(post("/notifications")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_notifications")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("sent")));
    }

    @Test
    @DisplayName("POST /notifications: должен вернуть already-processed при дубле")
    void notify_WhenDuplicateKey_ShouldReturnAlreadyProcessed() throws Exception {
        String key = "unique-key-123";
        NotificationRequest request = new NotificationRequest(
                NotificationType.WITHDRAW, "testuser", 300, "Списание");

        // Первый запрос
        mockMvc.perform(post("/notifications")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_notifications")))
                        .header("X-Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("sent")));

        // Второй запрос с тем же ключом
        mockMvc.perform(post("/notifications")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_notifications")))
                        .header("X-Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("already-processed")));
    }

    @Test
    @DisplayName("POST /notifications: должен работать без idempotencyKey")
    void notify_WithoutKey_ShouldWork() throws Exception {
        NotificationRequest request = new NotificationRequest(
                NotificationType.TRANSFER, "testuser", 1000, "Перевод");

        mockMvc.perform(post("/notifications")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "service"))
                                .authorities(new SimpleGrantedAuthority("ROLE_notifications")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("sent")));
    }

    @Test
    @DisplayName("POST /notifications: должен вернуть 403 без роли ROLE_notifications")
    void notify_WithoutRole_ShouldReturnForbidden() throws Exception {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT, "testuser", 100, "Test");

        mockMvc.perform(post("/notifications")
                        .with(jwt().jwt(jwt -> jwt.claim("preferred_username", "user"))
                                .authorities(new SimpleGrantedAuthority("ROLE_user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /notifications: должен вернуть 401 без аутентификации")
    void notify_Unauthenticated_ShouldReturnUnauthorized() throws Exception {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT, "testuser", 100, "Test");

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}