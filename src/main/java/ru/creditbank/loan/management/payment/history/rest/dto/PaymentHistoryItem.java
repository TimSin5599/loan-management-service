package ru.creditbank.loan.management.payment.history.rest.dto;

import ru.creditbank.loan.management.payment.dao.entity.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentHistoryItem(
        UUID paymentId,
        BigDecimal amount,
        OffsetDateTime paymentDate,
        PaymentType paymentType,
        BigDecimal newBalance
) {
}
