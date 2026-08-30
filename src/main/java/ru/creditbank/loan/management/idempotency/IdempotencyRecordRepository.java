package ru.creditbank.loan.management.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, UUID> {
    Optional<IdempotencyRecordEntity> findByIdempotencyKeyAndOperation(String idempotencyKey, String operation);
}
