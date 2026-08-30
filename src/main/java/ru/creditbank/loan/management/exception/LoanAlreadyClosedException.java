package ru.creditbank.loan.management.exception;

import java.util.UUID;

public class LoanAlreadyClosedException extends RuntimeException {
    public LoanAlreadyClosedException(UUID loanId) {
        super("Кредит уже закрыт и не может быть отменен: " + loanId);
    }
}
