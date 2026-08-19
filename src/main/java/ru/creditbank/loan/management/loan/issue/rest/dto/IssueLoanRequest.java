package ru.creditbank.loan.management.loan.issue.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record IssueLoanRequest(

        @NotNull(message = "Идентификатор кредитной заявки обязателен")
        UUID creditApplicationId,

        @NotNull(message = "Идентификатор пользователя обязателен")
        UUID userId,

        @NotNull(message = "Сумма кредита обязательна")
        @DecimalMin(value = "0.01", message = "Сумма кредита должна быть положительной")
        BigDecimal totalAmount,

        @NotNull(message = "Срок кредита обязателен")
        @Min(value = 1, message = "Срок кредита должен быть не менее 1 месяца")
        Integer termMonths,

        BigDecimal interestRate
) {
}
