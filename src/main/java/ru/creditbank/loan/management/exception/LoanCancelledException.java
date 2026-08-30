package ru.creditbank.loan.management.exception;

import java.util.UUID;

public class LoanCancelledException extends RuntimeException {
    public LoanCancelledException(UUID loanId) {
        super("Кредит отменен и не может быть изменен: " + loanId);
    }
}
