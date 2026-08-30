package ru.creditbank.loan.management.loan.schedule.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.creditbank.loan.management.exception.LoanCancelledException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.schedule.dao.entity.PaymentScheduleEntity;
import ru.creditbank.loan.management.loan.schedule.dao.repository.PaymentScheduleRepository;
import ru.creditbank.loan.management.loan.schedule.rest.dto.CreatePaymentScheduleResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleUseCaseImplTest {
    @Mock
    private LoanRepository loanRepository;

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @InjectMocks
    private PaymentScheduleUseCaseImpl paymentScheduleUseCase;

    @Test
    void createSchedule_freshLoanWithNonDivisibleAmount_generatesInstallmentsSummingToTotal() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .totalAmount(BigDecimal.valueOf(100000))
                .termMonths(3)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)).thenReturn(List.of());

        CreatePaymentScheduleResponse response = paymentScheduleUseCase.createSchedule(loanId);

        assertThat(response.loanId()).isEqualTo(loanId);
        assertThat(response.installmentsCreated()).isEqualTo(3);

        ArgumentCaptor<List<PaymentScheduleEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentScheduleRepository).saveAll(captor.capture());
        List<PaymentScheduleEntity> installments = captor.getValue();
        assertThat(installments).hasSize(3);

        BigDecimal sum = installments.stream()
                .map(PaymentScheduleEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(100000));

        // 100000 / 3 = 33333.33 (HALF_UP), last installment absorbs the remainder
        assertThat(installments.get(0).getAmount()).isEqualByComparingTo("33333.33");
        assertThat(installments.get(1).getAmount()).isEqualByComparingTo("33333.33");
        assertThat(installments.get(2).getAmount()).isEqualByComparingTo("33333.34");

        assertThat(installments.get(0).getInstallmentNumber()).isEqualTo(1);
        assertThat(installments.get(0).getDueDate()).isEqualTo(loan.getCreatedAt().toLocalDate().plusMonths(1));
        assertThat(installments.get(2).getDueDate()).isEqualTo(loan.getCreatedAt().toLocalDate().plusMonths(3));
    }

    @Test
    void createSchedule_scheduleAlreadyExists_returnsExistingWithoutSavingAgain() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .totalAmount(BigDecimal.valueOf(100000))
                .termMonths(3)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        List<PaymentScheduleEntity> existing = List.of(
                PaymentScheduleEntity.builder().installmentNumber(1).build(),
                PaymentScheduleEntity.builder().installmentNumber(2).build(),
                PaymentScheduleEntity.builder().installmentNumber(3).build());
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)).thenReturn(existing);

        CreatePaymentScheduleResponse response = paymentScheduleUseCase.createSchedule(loanId);

        assertThat(response.loanId()).isEqualTo(loanId);
        assertThat(response.installmentsCreated()).isEqualTo(3);
        verify(paymentScheduleRepository, never()).saveAll(any());
    }

    @Test
    void createSchedule_loanMissing_throwsLoanNotFoundException() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentScheduleUseCase.createSchedule(loanId))
                .isInstanceOf(LoanNotFoundException.class);
        verify(paymentScheduleRepository, never()).saveAll(any());
    }

    @Test
    void createSchedule_loanCancelled_throwsLoanCancelledException() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .totalAmount(BigDecimal.valueOf(100000))
                .termMonths(3)
                .status(LoanStatus.CANCELLED)
                .createdAt(LocalDateTime.now())
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)).thenReturn(List.of());

        assertThatThrownBy(() -> paymentScheduleUseCase.createSchedule(loanId))
                .isInstanceOf(LoanCancelledException.class);
        verify(paymentScheduleRepository, never()).saveAll(any());
    }

    @Test
    void createSchedule_concurrentDuplicateInsert_returnsWinnersScheduleInsteadOfThrowing() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .totalAmount(BigDecimal.valueOf(100000))
                .termMonths(3)
                .status(LoanStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        List<PaymentScheduleEntity> winner = List.of(
                PaymentScheduleEntity.builder().installmentNumber(1).build(),
                PaymentScheduleEntity.builder().installmentNumber(2).build(),
                PaymentScheduleEntity.builder().installmentNumber(3).build());
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId))
                .thenReturn(List.of())
                .thenReturn(winner);
        when(paymentScheduleRepository.saveAll(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        CreatePaymentScheduleResponse response = paymentScheduleUseCase.createSchedule(loanId);

        assertThat(response.loanId()).isEqualTo(loanId);
        assertThat(response.installmentsCreated()).isEqualTo(winner.size());
    }
}
