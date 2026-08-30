package ru.creditbank.loan.management.payment.history.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.service.LoanProvider;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryItem;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryItemMapper;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryResponse;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentHistoryUseCaseImpl implements PaymentHistoryUseCase {
    private static final Logger log = LoggerFactory.getLogger(PaymentHistoryUseCaseImpl.class);

    private final LoanProvider loanProvider;
    private final PaymentRepository paymentRepository;
    private final PaymentHistoryItemMapper paymentHistoryItemMapper;

    public PaymentHistoryUseCaseImpl(LoanProvider loanProvider, PaymentRepository paymentRepository,
                                      PaymentHistoryItemMapper paymentHistoryItemMapper) {
        this.loanProvider = loanProvider;
        this.paymentRepository = paymentRepository;
        this.paymentHistoryItemMapper = paymentHistoryItemMapper;
    }

    @Override
    public PaymentHistoryResponse getHistory(AuthenticatedUser user, UUID loanId) {
        LoanEntity loan = loanProvider.findById(loanId)
                .filter(l -> l.getUserId().equals(user.userId()))
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        List<PaymentHistoryItem> payments = paymentRepository.findAllByLoanIdOrderByPaymentDateDesc(loan.getId()).stream()
                .map(paymentHistoryItemMapper::toHistoryItem)
                .toList();

        log.info("Запрошена история платежей: userId={}, loanId={}, paymentCount={}",
                user.userId(), loanId, payments.size());

        return new PaymentHistoryResponse(payments);
    }
}
