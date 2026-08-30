package ru.creditbank.loan.management.exception;

import java.util.UUID;

public class LoanNotFoundException extends RuntimeException {
    public LoanNotFoundException(UUID loanId) {
        super("Кредит не найден: " + loanId);
    }
}
