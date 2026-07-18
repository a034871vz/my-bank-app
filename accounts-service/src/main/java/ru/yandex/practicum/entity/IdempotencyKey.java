package ru.yandex.practicum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", schema = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKey {

    @Id
    @Column(length = 100)
    private String key;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(name = "resulting_balance", nullable = false)
    private Integer resultingBalance;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IdempotencyKey(String key, String login, Integer resultingBalance) {
        this.key = key;
        this.login = login;
        this.resultingBalance = resultingBalance;
        this.createdAt = LocalDateTime.now();
    }
}