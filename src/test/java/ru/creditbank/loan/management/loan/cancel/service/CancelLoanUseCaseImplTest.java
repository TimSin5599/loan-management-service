package ru.creditbank.loan.management.loan.cancel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.loan.management.exception.LoanAlreadyClosedException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.cancel.rest.dto.CancelLoanResponse;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelLoanUseCaseImplTest {
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private CancelLoanUseCaseImpl cancelLoanUseCase;

    @Test
    void cancelLoan_activeLoan_setsStatusToCancelled() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .status(LoanStatus.ACTIVE)
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CancelLoanResponse response = cancelLoanUseCase.cancelLoan(loanId);

        assertThat(response.loanId()).isEqualTo(loanId);
        assertThat(response.status()).isEqualTo(LoanStatus.CANCELLED);
        ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
        verify(loanRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_overdueLoan_setsStatusToCancelled() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .status(LoanStatus.OVERDUE)
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CancelLoanResponse response = cancelLoanUseCase.cancelLoan(loanId);

        assertThat(response.status()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_alreadyCancelled_isIdempotentNoOp() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .status(LoanStatus.CANCELLED)
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        CancelLoanResponse response = cancelLoanUseCase.cancelLoan(loanId);

        assertThat(response.loanId()).isEqualTo(loanId);
        assertThat(response.status()).isEqualTo(LoanStatus.CANCELLED);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void cancelLoan_closedLoan_throwsLoanAlreadyClosedException() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(loanId)
                .status(LoanStatus.CLOSED)
                .build();
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> cancelLoanUseCase.cancelLoan(loanId))
                .isInstanceOf(LoanAlreadyClosedException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void cancelLoan_loanMissing_throwsLoanNotFoundException() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelLoanUseCase.cancelLoan(loanId))
                .isInstanceOf(LoanNotFoundException.class);
        verify(loanRepository, never()).save(any());
    }
}
