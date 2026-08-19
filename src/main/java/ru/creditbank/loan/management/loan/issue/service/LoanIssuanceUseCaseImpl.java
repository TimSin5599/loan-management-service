package ru.creditbank.loan.management.loan.issue.service;

import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;

import java.time.LocalDate;

@Service
public class LoanIssuanceUseCaseImpl implements LoanIssuanceUseCase {

    private final LoanRepository loanRepository;

    public LoanIssuanceUseCaseImpl(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public IssueLoanResponse issueLoan(IssueLoanRequest request) {
        LoanEntity existing = loanRepository.findByCreditApplicationId(request.creditApplicationId()).orElse(null);
        if (existing != null) {
            return new IssueLoanResponse(existing.getId(), existing.getStatus());
        }

        LoanEntity loan = LoanEntity.builder()
                .userId(request.userId())
                .creditApplicationId(request.creditApplicationId())
                .totalAmount(request.totalAmount())
                .remainingAmount(request.totalAmount())
                .interestRate(request.interestRate())
                .termMonths(request.termMonths())
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .build();

        LoanEntity saved = loanRepository.save(loan);
        return new IssueLoanResponse(saved.getId(), saved.getStatus());
    }
}
