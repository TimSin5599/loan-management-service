package ru.creditbank.loan.management.loan.issue.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanRequest;
import ru.creditbank.loan.management.loan.issue.rest.dto.IssueLoanResponse;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanIssuanceUseCaseImplTest {
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanIssuanceUseCaseImpl loanIssuanceUseCase;

    @Test
    void issueLoan_newApplication_createsLoan() {
        UUID creditApplicationId = UUID.randomUUID();
        when(loanRepository.findByCreditApplicationId(creditApplicationId)).thenReturn(Optional.empty());
        when(loanRepository.save(any())).thenAnswer(invocation -> {
            LoanEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        IssueLoanRequest request = new IssueLoanRequest(
                creditApplicationId, UUID.randomUUID(), BigDecimal.valueOf(100000), 24, BigDecimal.valueOf(12.5));
        IssueLoanResponse response = loanIssuanceUseCase.issueLoan(request);

        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        ArgumentCaptor<LoanEntity> captor = ArgumentCaptor.forClass(LoanEntity.class);
        verify(loanRepository).save(captor.capture());
        assertThat(captor.getValue().getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    @Test
    void issueLoan_existingApplication_returnsExistingLoanWithoutCreatingDuplicate() {
        UUID creditApplicationId = UUID.randomUUID();
        LoanEntity existing = LoanEntity.builder()
                .id(UUID.randomUUID())
                .creditApplicationId(creditApplicationId)
                .status(LoanStatus.ACTIVE)
                .build();
        when(loanRepository.findByCreditApplicationId(creditApplicationId)).thenReturn(Optional.of(existing));

        IssueLoanRequest request = new IssueLoanRequest(
                creditApplicationId, UUID.randomUUID(), BigDecimal.valueOf(100000), 24, null);
        IssueLoanResponse response = loanIssuanceUseCase.issueLoan(request);

        assertThat(response.loanId()).isEqualTo(existing.getId());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void issueLoan_concurrentDuplicateInsert_returnsWinnersLoanInsteadOfThrowing() {
        UUID creditApplicationId = UUID.randomUUID();
        LoanEntity winner = LoanEntity.builder()
                .id(UUID.randomUUID())
                .creditApplicationId(creditApplicationId)
                .status(LoanStatus.ACTIVE)
                .build();
        when(loanRepository.findByCreditApplicationId(creditApplicationId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(loanRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        IssueLoanRequest request = new IssueLoanRequest(
                creditApplicationId, UUID.randomUUID(), BigDecimal.valueOf(100000), 24, BigDecimal.valueOf(12.5));
        IssueLoanResponse response = loanIssuanceUseCase.issueLoan(request);

        assertThat(response.loanId()).isEqualTo(winner.getId());
        assertThat(response.status()).isEqualTo(winner.getStatus());
    }
}
