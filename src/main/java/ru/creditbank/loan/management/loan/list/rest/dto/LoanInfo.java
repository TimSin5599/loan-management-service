package ru.creditbank.loan.management.loan.list.rest.dto;

import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanInfo(
        UUID loanId,
        BigDecimal totalAmount,
        BigDecimal remainingAmount,
        LocalDate nextPaymentDate,
        LoanStatus status
) {
}
