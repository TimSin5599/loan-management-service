package ru.creditbank.loan.management.loan.creditsummary.service;

import ru.creditbank.loan.management.loan.creditsummary.rest.dto.CreditSummaryResponse;

import java.util.UUID;

public interface CreditSummaryUseCase {
    CreditSummaryResponse getCreditSummary(UUID userId);
}
