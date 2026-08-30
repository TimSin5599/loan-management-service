package ru.creditbank.loan.management.payment.history.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryResponse;
import ru.creditbank.loan.management.payment.history.service.PaymentHistoryUseCase;

import java.util.UUID;

@RestController
public class PaymentHistoryController {
    private final PaymentHistoryUseCase paymentHistoryUseCase;

    public PaymentHistoryController(PaymentHistoryUseCase paymentHistoryUseCase) {
        this.paymentHistoryUseCase = paymentHistoryUseCase;
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/loan-management-service/api/payment/history")
    public PaymentHistoryResponse getHistory(@AuthenticationPrincipal AuthenticatedUser user,
                                              @RequestParam UUID loanId) {
        return paymentHistoryUseCase.getHistory(user, loanId);
    }
}
