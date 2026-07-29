package ru.yandex.practicum.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.dto.NotificationRequest;
import ru.yandex.practicum.enums.NotificationType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Юнит тесты NotificationService")
class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    @DisplayName("sendNotification: должен вернуть false для нового ключа")
    void sendNotification_NewKey_ShouldReturnFalse() {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT, "user", 100, "msg");

        boolean result = notificationService.sendNotification(request, "key-1");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendNotification: должен вернуть true для дубля")
    void sendNotification_DuplicateKey_ShouldReturnTrue() {
        NotificationRequest request = new NotificationRequest(
                NotificationType.DEPOSIT, "user", 100, "msg");

        notificationService.sendNotification(request, "key-2");
        boolean result = notificationService.sendNotification(request, "key-2");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("sendNotification: должен игнорировать null ключ")
    void sendNotification_NullKey_ShouldNotCache() {
        NotificationRequest request = new NotificationRequest(
                NotificationType.WITHDRAW, "user", 200, "msg");

        boolean result1 = notificationService.sendNotification(request, null);
        boolean result2 = notificationService.sendNotification(request, null);

        assertThat(result1).isFalse();
        assertThat(result2).isFalse(); // Не кэширует null
    }

    @Test
    @DisplayName("sendNotification: должен игнорировать blank ключ")
    void sendNotification_BlankKey_ShouldNotCache() {
        NotificationRequest request = new NotificationRequest(
                NotificationType.TRANSFER, "user", 300, "msg");

        boolean result1 = notificationService.sendNotification(request, "   ");
        boolean result2 = notificationService.sendNotification(request, "   ");

        assertThat(result1).isFalse();
        assertThat(result2).isFalse();
    }
}