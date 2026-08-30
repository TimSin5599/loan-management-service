package ru.creditbank.loan.management.payment.history.rest.dto;

import org.springframework.stereotype.Component;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;

@Component
public class PaymentHistoryItemMapper {
    public PaymentHistoryItem toHistoryItem(PaymentEntity payment) {
        return new PaymentHistoryItem(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentType(),
                payment.getBalanceAfter()
        );
    }
}
