package ru.yandex.practicum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.yandex.practicum.dto.CashRequest;
import ru.yandex.practicum.enums.CashOperationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "cash_operations", schema = "cash")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CashOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private CashOperationType operationType;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public CashOperation(String login, CashRequest request) {
        this.login = login;
        this.operationType = request.type();
        this.amount = request.amount();
        this.createdAt = LocalDateTime.now();
    }
}