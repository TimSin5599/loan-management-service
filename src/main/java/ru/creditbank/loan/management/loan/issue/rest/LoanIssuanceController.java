package ru.creditbank.loan.management.loan.issue.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.idempotency.IdempotencyService;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;
import ru.creditbank.loan.management.loan.issue.service.LoanIssuanceUseCase;

import java.util.Optional;

@RestController
public class LoanIssuanceController {
    private static final String OPERATION = "issue-loan";

    private final LoanIssuanceUseCase loanIssuanceUseCase;
    private final IdempotencyService idempotencyService;

    public LoanIssuanceController(LoanIssuanceUseCase loanIssuanceUseCase, IdempotencyService idempotencyService) {
        this.loanIssuanceUseCase = loanIssuanceUseCase;
        this.idempotencyService = idempotencyService;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/loan-management-service/internal/loans")
    public IssueLoanResponse issueLoan(@Valid @RequestBody IssueLoanRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<IssueLoanResponse> cached =
                    idempotencyService.findCached(idempotencyKey, OPERATION, IssueLoanResponse.class);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        IssueLoanResponse response = loanIssuanceUseCase.issueLoan(request);

        if (idempotencyKey != null) {
            response = idempotencyService.remember(idempotencyKey, OPERATION, response);
        }
        return response;
    }
}
