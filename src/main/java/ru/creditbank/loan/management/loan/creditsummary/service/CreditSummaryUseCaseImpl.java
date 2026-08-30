package ru.creditbank.loan.management.loan.creditsummary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.loan.creditsummary.rest.dto.CreditSummaryResponse;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CreditSummaryUseCaseImpl implements CreditSummaryUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreditSummaryUseCaseImpl.class);

    private final LoanRepository loanRepository;

    public CreditSummaryUseCaseImpl(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public CreditSummaryResponse getCreditSummary(UUID userId) {
        List<LoanEntity> loans = loanRepository.findAllByUserId(userId);

        int activeLoans = (int) loans.stream()
                .filter(loan -> loan.getStatus() != LoanStatus.CLOSED)
                .count();
        boolean hasActiveOverdue = loans.stream()
                .anyMatch(loan -> loan.getStatus() == LoanStatus.OVERDUE);
        BigDecimal totalOutstandingDebt = loans.stream()
                .filter(loan -> loan.getStatus() != LoanStatus.CLOSED)
                .map(LoanEntity::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Рассчитана кредитная сводка: userId={}, totalLoans={}, activeLoans={}, hasActiveOverdue={}",
                userId, loans.size(), activeLoans, hasActiveOverdue);

        return new CreditSummaryResponse(loans.size(), activeLoans, hasActiveOverdue, totalOutstandingDebt);
    }
}
