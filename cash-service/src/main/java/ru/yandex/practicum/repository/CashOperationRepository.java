package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.entity.CashOperation;

@Repository
public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

}