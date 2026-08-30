package ru.creditbank.loan.management.loan.schedule.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.creditbank.loan.management.loan.schedule.dao.entity.PaymentScheduleEntity;

import java.util.List;
import java.util.UUID;

public interface PaymentScheduleRepository extends JpaRepository<PaymentScheduleEntity, UUID> {
    List<PaymentScheduleEntity> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);
}
