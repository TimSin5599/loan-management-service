package ru.creditbank.loan.management.loan.issue.service;

import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;

public interface LoanIssuanceUseCase {
    IssueLoanResponse issueLoan(IssueLoanRequest request);
}
