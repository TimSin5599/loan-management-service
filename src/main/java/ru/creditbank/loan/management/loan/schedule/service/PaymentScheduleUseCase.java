package ru.creditbank.loan.management.loan.schedule.service;

import ru.creditbank.loan.management.loan.schedule.rest.dto.CreatePaymentScheduleResponse;

import java.util.UUID;

public interface PaymentScheduleUseCase {
    CreatePaymentScheduleResponse createSchedule(UUID loanId);
}
