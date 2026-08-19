package ru.creditbank.loan.management.payment.create.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(UUID paymentId, BigDecimal newBalance, LocalDate nextPaymentDate) {
}
