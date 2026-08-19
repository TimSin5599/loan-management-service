package ru.creditbank.loan.management.loan.list.rest.dto;

import java.util.List;

public record UserLoansResponse(List<LoanInfo> loans) {
}
