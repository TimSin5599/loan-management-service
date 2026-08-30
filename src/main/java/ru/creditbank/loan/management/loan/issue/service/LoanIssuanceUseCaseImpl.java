package ru.creditbank.loan.management.loan.issue.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;

import java.time.LocalDate;

@Service
public class LoanIssuanceUseCaseImpl implements LoanIssuanceUseCase {
    private static final Logger log = LoggerFactory.getLogger(LoanIssuanceUseCaseImpl.class);

    private final LoanRepository loanRepository;

    public LoanIssuanceUseCaseImpl(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    @Transactional
    public IssueLoanResponse issueLoan(IssueLoanRequest request) {
        LoanEntity existing = loanRepository.findByCreditApplicationId(request.creditApplicationId()).orElse(null);
        if (existing != null) {
            log.info("Кредитная заявка уже обработана, возвращаем существующий кредит: creditApplicationId={}, loanId={}",
                    request.creditApplicationId(), existing.getId());
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

        LoanEntity saved;
        try {
            saved = loanRepository.save(loan);
        } catch (DataIntegrityViolationException ex) {
            log.info("Гонка при создании кредита, возвращаем кредит конкурирующего запроса: creditApplicationId={}",
                    request.creditApplicationId());
            LoanEntity winner = loanRepository.findByCreditApplicationId(request.creditApplicationId())
                    .orElseThrow(() -> ex);
            return new IssueLoanResponse(winner.getId(), winner.getStatus());
        }
        log.info("Создан новый кредит: loanId={}, creditApplicationId={}, userId={}",
                saved.getId(), request.creditApplicationId(), request.userId());
        return new IssueLoanResponse(saved.getId(), saved.getStatus());
    }
}
