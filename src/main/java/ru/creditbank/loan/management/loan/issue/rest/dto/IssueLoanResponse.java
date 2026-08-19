package ru.creditbank.loan.management.loan.issue.rest.dto;

import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;

import java.util.UUID;

public record IssueLoanResponse(UUID loanId, LoanStatus status) {
}
