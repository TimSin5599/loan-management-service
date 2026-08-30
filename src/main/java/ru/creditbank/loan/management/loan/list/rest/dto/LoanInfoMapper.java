package ru.creditbank.loan.management.loan.list.rest.dto;

import org.springframework.stereotype.Component;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;

@Component
public class LoanInfoMapper {
    public LoanInfo toLoanInfo(LoanEntity loan) {
        return new LoanInfo(
                loan.getId(),
                loan.getTotalAmount(),
                loan.getRemainingAmount(),
                loan.getNextPaymentDate(),
                loan.getStatus()
        );
    }
}
