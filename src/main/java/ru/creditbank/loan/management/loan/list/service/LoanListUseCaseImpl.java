package ru.creditbank.loan.management.loan.list.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.list.rest.dto.LoanInfo;
import ru.creditbank.loan.management.loan.list.rest.dto.LoanInfoMapper;
import ru.creditbank.loan.management.loan.list.rest.dto.UserLoansResponse;

import java.util.List;

@Service
public class LoanListUseCaseImpl implements LoanListUseCase {
    private static final Logger log = LoggerFactory.getLogger(LoanListUseCaseImpl.class);

    private final LoanRepository loanRepository;
    private final LoanInfoMapper loanInfoMapper;

    public LoanListUseCaseImpl(LoanRepository loanRepository, LoanInfoMapper loanInfoMapper) {
        this.loanRepository = loanRepository;
        this.loanInfoMapper = loanInfoMapper;
    }

    @Override
    public UserLoansResponse getUserLoans(AuthenticatedUser user) {
        List<LoanInfo> loans = loanRepository.findAllByUserId(user.userId()).stream()
                .map(loanInfoMapper::toLoanInfo)
                .toList();

        log.info("Запрошен список кредитов: userId={}, loanCount={}", user.userId(), loans.size());

        return new UserLoansResponse(loans);
    }
}
