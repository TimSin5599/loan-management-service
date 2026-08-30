package ru.creditbank.loan.management.payment.create.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(PaymentCreateUseCaseImpl.class);

    private final LoanProvider loanProvider;
    private final PaymentRepository paymentRepository;

    public PaymentCreateUseCaseImpl(LoanProvider loanProvider, PaymentRepository paymentRepository) {
        this.loanProvider = loanProvider;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(AuthenticatedUser user, CreatePaymentRequest request) {
        LoanEntity loan = loanProvider.findByIdForUpdate(request.loanId())
                .filter(l -> l.getUserId().equals(user.userId()))
                .orElseThrow(() -> {
                    LoanNotFoundException ex = new LoanNotFoundException(request.loanId());
                    log.warn("Платеж отклонен, кредит не найден: loanId={}, message={}", request.loanId(), ex.getMessage());
                    return ex;
                });

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            LoanNotActiveException ex = new LoanNotActiveException(loan.getId(), loan.getStatus());
            log.warn("Платеж отклонен, кредит неактивен: loanId={}, message={}", loan.getId(), ex.getMessage());
            throw ex;
        }

        BigDecimal amount = request.amount();
        if (amount.compareTo(loan.getRemainingAmount()) > 0) {
            PaymentExceedsBalanceException ex = new PaymentExceedsBalanceException(loan.getId(), amount, loan.getRemainingAmount());
            log.warn("Платеж отклонен, сумма превышает остаток задолженности: loanId={}, message={}", loan.getId(), ex.getMessage());
            throw ex;
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

        log.info("Зафиксирован платеж: paymentId={}, loanId={}, paymentType={}, balanceAfter={}",
                savedPayment.getId(), savedLoan.getId(), request.paymentType(), savedLoan.getRemainingAmount());

        return new PaymentResponse(savedPayment.getId(), savedLoan.getRemainingAmount(), savedLoan.getNextPaymentDate());
    }
}
