package ru.creditbank.loan.management.loan.dao.service;

import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class LoanProvider {

    private final LoanRepository loanRepository;

    public LoanProvider(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    public Optional<LoanEntity> findById(UUID id) {
        return loanRepository.findById(id);
    }

    public LoanEntity save(LoanEntity loan) {
        return loanRepository.save(loan);
    }
}
