package ru.creditbank.loan.management.payment.history.rest.dto;

import java.util.List;

public record PaymentHistoryResponse(List<PaymentHistoryItem> payments) {
}
