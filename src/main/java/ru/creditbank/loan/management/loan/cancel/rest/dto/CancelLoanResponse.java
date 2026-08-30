package ru.creditbank.loan.management.loan.cancel.rest.dto;

import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;

import java.util.UUID;

public record CancelLoanResponse(UUID loanId, LoanStatus status) {
}
