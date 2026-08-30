package ru.creditbank.loan.management.loan.schedule.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.creditbank.loan.management.exception.LoanCancelledException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.schedule.dao.entity.PaymentScheduleEntity;
import ru.creditbank.loan.management.loan.schedule.dao.repository.PaymentScheduleRepository;
import ru.creditbank.loan.management.loan.schedule.rest.dto.CreatePaymentScheduleResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Due dates are anchored to the loan's creation date ({@link LoanEntity#getCreatedAt()}) rather than
 * {@link LocalDate#now()}, so that regenerating the schedule reasoning stays deterministic relative to
 * when the loan itself was issued.
 */
@Service
public class PaymentScheduleUseCaseImpl implements PaymentScheduleUseCase {
    private static final Logger log = LoggerFactory.getLogger(PaymentScheduleUseCaseImpl.class);

    private final LoanRepository loanRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;

    public PaymentScheduleUseCaseImpl(LoanRepository loanRepository, PaymentScheduleRepository paymentScheduleRepository) {
        this.loanRepository = loanRepository;
        this.paymentScheduleRepository = paymentScheduleRepository;
    }

    @Override
    @Transactional
    public CreatePaymentScheduleResponse createSchedule(UUID loanId) {
        LoanEntity loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));

        List<PaymentScheduleEntity> existing = paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        if (!existing.isEmpty()) {
            log.info("График платежей уже существует, возвращаем существующий: loanId={}, installmentsCreated={}",
                    loanId, existing.size());
            return new CreatePaymentScheduleResponse(loanId, existing.size());
        }

        if (loan.getStatus() == LoanStatus.CANCELLED) {
            throw new LoanCancelledException(loanId);
        }

        int termMonths = loan.getTermMonths();
        BigDecimal[] installmentAmounts = calculateInstallmentAmounts(loan.getTotalAmount(), termMonths);
        LocalDate baseDate = loan.getCreatedAt() != null ? loan.getCreatedAt().toLocalDate() : LocalDate.now();

        List<PaymentScheduleEntity> installments = new ArrayList<>(termMonths);
        for (int i = 0; i < termMonths; i++) {
            installments.add(PaymentScheduleEntity.builder()
                    .loanId(loanId)
                    .dueDate(baseDate.plusMonths(i + 1))
                    .amount(installmentAmounts[i])
                    .installmentNumber(i + 1)
                    .build());
        }
        try {
            paymentScheduleRepository.saveAll(installments);
        } catch (DataIntegrityViolationException ex) {
            log.info("Гонка при создании графика платежей, возвращаем существующий график: loanId={}", loanId);
            List<PaymentScheduleEntity> winner = paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
            if (winner.isEmpty()) {
                throw ex;
            }
            return new CreatePaymentScheduleResponse(loanId, winner.size());
        }

        log.info("Создан график платежей: loanId={}, installmentsCreated={}", loanId, installments.size());
        return new CreatePaymentScheduleResponse(loanId, installments.size());
    }

    private BigDecimal[] calculateInstallmentAmounts(BigDecimal totalAmount, int termMonths) {
        BigDecimal[] amounts = new BigDecimal[termMonths];
        BigDecimal perInstallment = totalAmount.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (int i = 0; i < termMonths - 1; i++) {
            amounts[i] = perInstallment;
            runningTotal = runningTotal.add(perInstallment);
        }
        amounts[termMonths - 1] = totalAmount.subtract(runningTotal);
        return amounts;
    }
}
