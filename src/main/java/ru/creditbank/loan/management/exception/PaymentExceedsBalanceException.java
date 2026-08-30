package ru.creditbank.loan.management.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentExceedsBalanceException extends RuntimeException {
    public PaymentExceedsBalanceException(UUID loanId, BigDecimal amount, BigDecimal remainingAmount) {
        super("Сумма платежа %s превышает остаток задолженности %s по кредиту %s"
                .formatted(amount, remainingAmount, loanId));
    }
}
