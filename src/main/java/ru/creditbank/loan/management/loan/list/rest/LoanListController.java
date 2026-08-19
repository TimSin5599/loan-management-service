package ru.creditbank.loan.management.loan.list.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.loan.list.rest.dto.UserLoansResponse;
import ru.creditbank.loan.management.loan.list.service.LoanListUseCase;

@RestController
public class LoanListController {

    private final LoanListUseCase loanListUseCase;

    public LoanListController(LoanListUseCase loanListUseCase) {
        this.loanListUseCase = loanListUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/loan-management-service/api/v1/payment/loans")
    public UserLoansResponse getUserLoans(@AuthenticationPrincipal AuthenticatedUser user) {
        return loanListUseCase.getUserLoans(user);
    }
}
