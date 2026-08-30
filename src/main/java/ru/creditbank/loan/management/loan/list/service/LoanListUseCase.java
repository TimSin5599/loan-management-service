package ru.creditbank.loan.management.loan.list.service;

import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.loan.list.rest.dto.UserLoansResponse;

public interface LoanListUseCase {
    UserLoansResponse getUserLoans(AuthenticatedUser user);
}
