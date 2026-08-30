package ru.creditbank.loan.management.payment.create.service;

import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.payment.create.rest.dto.CreatePaymentRequest;
import ru.creditbank.loan.management.payment.create.rest.dto.PaymentResponse;

public interface PaymentCreateUseCase {
    PaymentResponse createPayment(AuthenticatedUser user, CreatePaymentRequest request);
}
