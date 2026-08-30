package ru.creditbank.loan.management.loan.creditsummary.rest.dto;

import java.math.BigDecimal;

public record CreditSummaryResponse(
        int totalLoans,
        int activeLoans,
        boolean hasActiveOverdue,
        BigDecimal totalOutstandingDebt
) {
}
