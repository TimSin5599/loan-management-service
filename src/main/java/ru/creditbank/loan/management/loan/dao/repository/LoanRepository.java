package ru.creditbank.loan.management.loan.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {

    List<LoanEntity> findAllByUserId(UUID userId);

    Optional<LoanEntity> findByCreditApplicationId(UUID creditApplicationId);
}
