package ru.creditbank.loan.management.loan.list.service;

import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.list.rest.dto.LoanInfo;
import ru.creditbank.loan.management.loan.list.rest.dto.UserLoansResponse;

import java.util.List;

@Service
public class LoanListUseCaseImpl implements LoanListUseCase {

    private final LoanRepository loanRepository;

    public LoanListUseCaseImpl(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public UserLoansResponse getUserLoans(AuthenticatedUser user) {
        List<LoanInfo> loans = loanRepository.findAllByUserId(user.userId()).stream()
                .map(this::toLoanInfo)
                .toList();
        return new UserLoansResponse(loans);
    }

    private LoanInfo toLoanInfo(LoanEntity loan) {
        return new LoanInfo(
                loan.getId(),
                loan.getTotalAmount(),
                loan.getRemainingAmount(),
                loan.getNextPaymentDate(),
                loan.getStatus()
        );
    }
}
