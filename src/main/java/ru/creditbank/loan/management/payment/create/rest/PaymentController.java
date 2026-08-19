package ru.creditbank.loan.management.payment.create.rest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.payment.create.rest.dto.CreatePaymentRequest;
import ru.creditbank.loan.management.payment.create.rest.dto.PaymentResponse;
import ru.creditbank.loan.management.payment.create.service.PaymentCreateUseCase;

@RestController
public class PaymentController {

    private final PaymentCreateUseCase paymentCreateUseCase;

    public PaymentController(PaymentCreateUseCase paymentCreateUseCase) {
        this.paymentCreateUseCase = paymentCreateUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/loan-management-service/api/v1/payment")
    public PaymentResponse createPayment(@AuthenticationPrincipal AuthenticatedUser user,
                                          @Valid @RequestBody CreatePaymentRequest request) {
        return paymentCreateUseCase.createPayment(user, request);
    }
}
