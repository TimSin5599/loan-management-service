package ru.creditbank.loan.management.payment.create.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ru.creditbank.loan.management.payment.dao.entity.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(

        @NotNull(message = "Идентификатор кредита обязателен")
        UUID loanId,

        @NotNull(message = "Сумма платежа обязательна")
        @DecimalMin(value = "0.01", message = "Сумма платежа должна быть положительной")
        BigDecimal amount,

        @NotNull(message = "Тип платежа обязателен")
        PaymentType paymentType
) {
}
