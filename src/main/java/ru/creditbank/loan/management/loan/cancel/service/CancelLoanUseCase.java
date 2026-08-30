package ru.creditbank.loan.management.loan.cancel.service;

import ru.creditbank.loan.management.loan.cancel.rest.dto.CancelLoanResponse;

import java.util.UUID;

public interface CancelLoanUseCase {
    CancelLoanResponse cancelLoan(UUID loanId);
}
