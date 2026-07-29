package ru.yandex.practicum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfer_operations", schema = "transfer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_login", nullable = false, length = 50)
    private String senderLogin;

    @Column(name = "recipient_login", nullable = false, length = 50)
    private String recipientLogin;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TransferOperation(String senderLogin, String recipientLogin, Integer amount) {
        this.senderLogin = senderLogin;
        this.recipientLogin = recipientLogin;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}