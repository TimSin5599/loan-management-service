package ru.creditbank.loan.management.payment.history.service;

import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryResponse;

import java.util.UUID;

public interface PaymentHistoryUseCase {
    PaymentHistoryResponse getHistory(AuthenticatedUser user, UUID loanId);
}
