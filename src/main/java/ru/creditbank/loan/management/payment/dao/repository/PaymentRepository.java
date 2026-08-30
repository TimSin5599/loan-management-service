package ru.creditbank.loan.management.payment.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    List<PaymentEntity> findAllByLoanIdOrderByPaymentDateDesc(UUID loanId);
}
