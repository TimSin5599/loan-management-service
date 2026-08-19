package ru.creditbank.loan.management.loan.issue.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;
import ru.creditbank.loan.management.loan.issue.service.LoanIssuanceUseCase;

@RestController
public class LoanIssuanceController {

    private final LoanIssuanceUseCase loanIssuanceUseCase;

    public LoanIssuanceController(LoanIssuanceUseCase loanIssuanceUseCase) {
        this.loanIssuanceUseCase = loanIssuanceUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/loan-management-service/internal/loans")
    public IssueLoanResponse issueLoan(@Valid @RequestBody IssueLoanRequest request) {
        return loanIssuanceUseCase.issueLoan(request);
    }
}
