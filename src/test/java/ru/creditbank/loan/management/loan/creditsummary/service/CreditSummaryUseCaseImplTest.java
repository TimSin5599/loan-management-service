package ru.creditbank.loan.management.loan.creditsummary.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.loan.management.loan.creditsummary.rest.dto.CreditSummaryResponse;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditSummaryUseCaseImplTest {
    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private CreditSummaryUseCaseImpl creditSummaryUseCase;

    @Test
    void getCreditSummary_noLoans_returnsZeroedSummary() {
        UUID userId = UUID.randomUUID();
        when(loanRepository.findAllByUserId(userId)).thenReturn(List.of());

        CreditSummaryResponse summary = creditSummaryUseCase.getCreditSummary(userId);

        assertThat(summary.totalLoans()).isZero();
        assertThat(summary.activeLoans()).isZero();
        assertThat(summary.hasActiveOverdue()).isFalse();
        assertThat(summary.totalOutstandingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getCreditSummary_mixOfStatuses_aggregatesOnlyNonClosedLoans() {
        UUID userId = UUID.randomUUID();
        List<LoanEntity> loans = List.of(
                loan(userId, LoanStatus.ACTIVE, BigDecimal.valueOf(50_000)),
                loan(userId, LoanStatus.OVERDUE, BigDecimal.valueOf(30_000)),
                loan(userId, LoanStatus.CLOSED, BigDecimal.ZERO)
        );
        when(loanRepository.findAllByUserId(userId)).thenReturn(loans);

        CreditSummaryResponse summary = creditSummaryUseCase.getCreditSummary(userId);

        assertThat(summary.totalLoans()).isEqualTo(3);
        assertThat(summary.activeLoans()).isEqualTo(2);
        assertThat(summary.hasActiveOverdue()).isTrue();
        assertThat(summary.totalOutstandingDebt()).isEqualByComparingTo(BigDecimal.valueOf(80_000));
    }

    @Test
    void getCreditSummary_onlyClosedLoans_noActiveOverdueAndZeroDebt() {
        UUID userId = UUID.randomUUID();
        when(loanRepository.findAllByUserId(userId))
                .thenReturn(List.of(loan(userId, LoanStatus.CLOSED, BigDecimal.ZERO)));

        CreditSummaryResponse summary = creditSummaryUseCase.getCreditSummary(userId);

        assertThat(summary.totalLoans()).isEqualTo(1);
        assertThat(summary.activeLoans()).isZero();
        assertThat(summary.hasActiveOverdue()).isFalse();
        assertThat(summary.totalOutstandingDebt()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private LoanEntity loan(UUID userId, LoanStatus status, BigDecimal remainingAmount) {
        return LoanEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(100_000))
                .remainingAmount(remainingAmount)
                .termMonths(12)
                .status(status)
                .build();
    }
}
