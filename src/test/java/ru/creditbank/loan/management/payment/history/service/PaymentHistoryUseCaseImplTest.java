package ru.creditbank.loan.management.payment.history.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.service.LoanProvider;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;
import ru.creditbank.loan.management.payment.dao.entity.PaymentType;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryItemMapper;
import ru.creditbank.loan.management.payment.history.rest.dto.PaymentHistoryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryUseCaseImplTest {
    @Mock
    private LoanProvider loanProvider;

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentHistoryUseCaseImpl paymentHistoryUseCase;

    @BeforeEach
    void setUp() {
        paymentHistoryUseCase = new PaymentHistoryUseCaseImpl(loanProvider, paymentRepository, new PaymentHistoryItemMapper());
    }

    @Test
    void getHistory_ownerRequestsOwnLoan_returnsPayments() {
        UUID userId = UUID.randomUUID();
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder().id(loanId).userId(userId).status(LoanStatus.ACTIVE).build();
        PaymentEntity payment = PaymentEntity.builder()
                .id(UUID.randomUUID())
                .loanId(loanId)
                .amount(BigDecimal.valueOf(5000))
                .paymentType(PaymentType.PARTIAL)
                .balanceAfter(BigDecimal.valueOf(95000))
                .build();
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));
        when(paymentRepository.findAllByLoanIdOrderByPaymentDateDesc(loanId)).thenReturn(List.of(payment));

        PaymentHistoryResponse response = paymentHistoryUseCase.getHistory(
                new AuthenticatedUser(userId, "u@example.com", null), loanId);

        assertThat(response.payments()).hasSize(1);
        assertThat(response.payments().get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    void getHistory_forSomeoneElsesLoan_throwsNotFound() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = LoanEntity.builder().id(loanId).userId(UUID.randomUUID()).status(LoanStatus.ACTIVE).build();
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> paymentHistoryUseCase.getHistory(
                new AuthenticatedUser(UUID.randomUUID(), "other@example.com", null), loanId))
                .isInstanceOf(LoanNotFoundException.class);
    }
}
