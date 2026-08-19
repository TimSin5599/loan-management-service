package ru.creditbank.loan.management.payment.create.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.exception.LoanNotActiveException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.exception.PaymentExceedsBalanceException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.service.LoanProvider;
import ru.creditbank.loan.management.payment.create.rest.dto.CreatePaymentRequest;
import ru.creditbank.loan.management.payment.create.rest.dto.PaymentResponse;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;

import java.math.BigDecimal;

@Service
public class PaymentCreateUseCaseImpl implements PaymentCreateUseCase {

    private final LoanProvider loanProvider;
    private final PaymentRepository paymentRepository;

    public PaymentCreateUseCaseImpl(LoanProvider loanProvider, PaymentRepository paymentRepository) {
        this.loanProvider = loanProvider;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, CreatePaymentRequest request) {
        LoanEntity loan = loanProvider.findById(request.loanId())
                .filter(l -> l.getUserId().equals(user.userId()))
                .orElseThrow(() -> new LoanNotFoundException(request.loanId()));

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new LoanNotActiveException(loan.getId(), loan.getStatus());
        }

        BigDecimal amount = request.amount();
        if (amount.compareTo(loan.getRemainingAmount()) > 0) {
            throw new PaymentExceedsBalanceException(loan.getId(), amount, loan.getRemainingAmount());
        }

        BigDecimal newRemaining = loan.getRemainingAmount().subtract(amount);
        loan.setRemainingAmount(newRemaining);
        if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
        } else {
            loan.setNextPaymentDate(loan.getNextPaymentDate().plusMonths(1));
        }
        LoanEntity savedLoan = loanProvider.save(loan);

        PaymentEntity payment = PaymentEntity.builder()
                .loanId(savedLoan.getId())
                .amount(amount)
                .paymentType(request.paymentType())
                .balanceAfter(savedLoan.getRemainingAmount())
                .build();
        PaymentEntity savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(savedPayment.getId(), savedLoan.getRemainingAmount(), savedLoan.getNextPaymentDate());
    }
}
