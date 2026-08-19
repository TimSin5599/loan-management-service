package ru.creditbank.loan.management.exception;

import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;

import java.util.UUID;

public class LoanNotActiveException extends RuntimeException {

    public LoanNotActiveException(UUID loanId, LoanStatus status) {
        super("Кредит %s находится в статусе %s, платежи по нему не принимаются".formatted(loanId, status));
    }
}
