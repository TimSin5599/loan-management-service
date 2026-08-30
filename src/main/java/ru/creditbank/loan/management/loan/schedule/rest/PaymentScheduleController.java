package ru.creditbank.loan.management.loan.schedule.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.creditbank.loan.management.idempotency.IdempotencyService;
import ru.creditbank.loan.management.loan.schedule.rest.dto.CreatePaymentScheduleResponse;
import ru.creditbank.loan.management.loan.schedule.service.PaymentScheduleUseCase;

import java.util.Optional;
import java.util.UUID;

@RestController
public class PaymentScheduleController {
    private static final String OPERATION = "create-schedule";

    private final PaymentScheduleUseCase paymentScheduleUseCase;
    private final IdempotencyService idempotencyService;

    public PaymentScheduleController(PaymentScheduleUseCase paymentScheduleUseCase, IdempotencyService idempotencyService) {
        this.paymentScheduleUseCase = paymentScheduleUseCase;
        this.idempotencyService = idempotencyService;
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/loan-management-service/internal/loans/{loanId}/schedule")
    public CreatePaymentScheduleResponse createSchedule(@PathVariable UUID loanId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<CreatePaymentScheduleResponse> cached =
                    idempotencyService.findCached(idempotencyKey, OPERATION, CreatePaymentScheduleResponse.class);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        CreatePaymentScheduleResponse response = paymentScheduleUseCase.createSchedule(loanId);

        if (idempotencyKey != null) {
            response = idempotencyService.remember(idempotencyKey, OPERATION, response);
        }
        return response;
    }
}
