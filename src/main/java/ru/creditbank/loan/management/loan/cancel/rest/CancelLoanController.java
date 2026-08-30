package ru.creditbank.loan.management.loan.cancel.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.idempotency.IdempotencyService;
import ru.creditbank.loan.management.loan.cancel.rest.dto.CancelLoanResponse;
import ru.creditbank.loan.management.loan.cancel.service.CancelLoanUseCase;

import java.util.Optional;
import java.util.UUID;

@RestController
public class CancelLoanController {
    private static final String OPERATION = "cancel-loan";

    private final CancelLoanUseCase cancelLoanUseCase;
    private final IdempotencyService idempotencyService;

    public CancelLoanController(CancelLoanUseCase cancelLoanUseCase, IdempotencyService idempotencyService) {
        this.cancelLoanUseCase = cancelLoanUseCase;
        this.idempotencyService = idempotencyService;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/loan-management-service/internal/loans/{loanId}/cancel")
    public CancelLoanResponse cancelLoan(@PathVariable UUID loanId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<CancelLoanResponse> cached =
                    idempotencyService.findCached(idempotencyKey, OPERATION, CancelLoanResponse.class);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        CancelLoanResponse response = cancelLoanUseCase.cancelLoan(loanId);

        if (idempotencyKey != null) {
            response = idempotencyService.remember(idempotencyKey, OPERATION, response);
        }
        return response;
    }
}
