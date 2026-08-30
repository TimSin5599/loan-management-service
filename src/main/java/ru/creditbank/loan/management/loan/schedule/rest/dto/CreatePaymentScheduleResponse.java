package ru.creditbank.loan.management.loan.schedule.rest.dto;

import java.util.UUID;

public record CreatePaymentScheduleResponse(UUID loanId, int installmentsCreated) {
}
