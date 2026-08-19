package ru.creditbank.loan.management.loan.list.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.list.rest.dto.UserLoansResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanListUseCaseImplTest {

    @Mock
    private LoanRepository loanRepository;

    @InjectMocks
    private LoanListUseCaseImpl loanListUseCase;

    @Test
    void getUserLoans_mapsEntitiesToLoanInfo() {
        UUID userId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(150000))
                .remainingAmount(BigDecimal.valueOf(120000))
                .nextPaymentDate(LocalDate.of(2024, 1, 15))
                .termMonths(12)
                .status(LoanStatus.ACTIVE)
                .build();
        when(loanRepository.findAllByUserId(userId)).thenReturn(List.of(loan));

        UserLoansResponse response = loanListUseCase.getUserLoans(new AuthenticatedUser(userId, "u@example.com", null));

        assertThat(response.loans()).hasSize(1);
        assertThat(response.loans().get(0).loanId()).isEqualTo(loan.getId());
        assertThat(response.loans().get(0).remainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(120000));
    }
}
