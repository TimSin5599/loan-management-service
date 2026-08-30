package ru.creditbank.loan.management.loan.creditsummary.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.loan.creditsummary.rest.dto.CreditSummaryResponse;
import ru.creditbank.loan.management.loan.creditsummary.service.CreditSummaryUseCase;

import java.util.UUID;

@RestController
public class CreditSummaryController {
    private final CreditSummaryUseCase creditSummaryUseCase;

    public CreditSummaryController(CreditSummaryUseCase creditSummaryUseCase) {
        this.creditSummaryUseCase = creditSummaryUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/loan-management-service/internal/users/{userId}/payment-history")
    public CreditSummaryResponse getCreditSummary(@PathVariable UUID userId) {
        return creditSummaryUseCase.getCreditSummary(userId);
    }
}
