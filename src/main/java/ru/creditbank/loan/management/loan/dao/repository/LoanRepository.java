package ru.creditbank.loan.management.loan.dao.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    List<LoanEntity> findAllByUserId(UUID userId);

    Optional<LoanEntity> findByCreditApplicationId(UUID creditApplicationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoanEntity l where l.id = :id")
    Optional<LoanEntity> findByIdForUpdate(@Param("id") UUID id);
}
