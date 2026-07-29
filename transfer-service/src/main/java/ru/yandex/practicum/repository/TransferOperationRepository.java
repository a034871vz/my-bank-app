package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.entity.TransferOperation;

@Repository
public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

}