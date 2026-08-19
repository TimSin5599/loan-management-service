package ru.creditbank.loan.management.payment.history.service;

import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.service.LoanProvider;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryItem;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryResponse;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentHistoryUseCaseImpl implements PaymentHistoryUseCase {

    private final LoanProvider loanProvider;
    private final PaymentRepository paymentRepository;

    public PaymentHistoryUseCaseImpl(LoanProvider loanProvider, PaymentRepository paymentRepository) {
        this.loanProvider = loanProvider;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentHistoryResponse getHistory(AuthenticatedUser user, UUID loanId) {
        LoanEntity loan = loanProvider.findById(loanId)
                .filter(l -> l.getUserId().equals(user.userId()))
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        List<PaymentHistoryItem> payments = paymentRepository.findAllByLoanIdOrderByPaymentDateDesc(loan.getId()).stream()
                .map(this::toHistoryItem)
                .toList();
        return new PaymentHistoryResponse(payments);
    }

    private PaymentHistoryItem toHistoryItem(PaymentEntity payment) {
        return new PaymentHistoryItem(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentType(),
                payment.getBalanceAfter()
        );
    }
}
