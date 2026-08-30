package ru.creditbank.loan.management.loan.cancel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.loan.management.exception.LoanAlreadyClosedException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.cancel.rest.dto.CancelLoanResponse;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;

import java.util.UUID;

@Service
public class CancelLoanUseCaseImpl implements CancelLoanUseCase {
    private static final Logger log = LoggerFactory.getLogger(CancelLoanUseCaseImpl.class);

    private final LoanRepository loanRepository;

    public CancelLoanUseCaseImpl(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    @Transactional
    public CancelLoanResponse cancelLoan(UUID loanId) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        if (loan.getStatus() == LoanStatus.CANCELLED) {
            log.info("Кредит уже отменен, повторная отмена проигнорирована: loanId={}", loanId);
            return new CancelLoanResponse(loan.getId(), loan.getStatus());
        }

        if (loan.getStatus() == LoanStatus.CLOSED) {
            throw new LoanAlreadyClosedException(loanId);
        }

        loan.setStatus(LoanStatus.CANCELLED);
        LoanEntity saved = loanRepository.save(loan);
        log.info("Кредит отменен: loanId={}", loanId);
        return new CancelLoanResponse(saved.getId(), saved.getStatus());
    }
}
