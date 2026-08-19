package ru.creditbank.loan.management.payment.create.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.creditbank.loan.management.config.AuthenticatedUser;
import ru.creditbank.loan.management.exception.LoanNotActiveException;
import ru.creditbank.loan.management.exception.LoanNotFoundException;
import ru.creditbank.loan.management.exception.PaymentExceedsBalanceException;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.service.LoanProvider;
import ru.creditbank.loan.management.payment.create.rest.dto.CreatePaymentRequest;
import ru.creditbank.loan.management.payment.create.rest.dto.PaymentResponse;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;
import ru.creditbank.loan.management.payment.dao.entity.PaymentType;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCreateUseCaseImplTest {

    @Mock
    private LoanProvider loanProvider;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentCreateUseCaseImpl paymentCreateUseCase;

    private final UUID userId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(userId, "user@example.com", null);

    @Test
    void createPayment_partial_updatesRemainingAmountAndAdvancesNextPaymentDate() {
        UUID loanId = UUID.randomUUID();
        LocalDate initialNextPaymentDate = LocalDate.of(2024, 1, 15);
        LoanEntity loan = loan(loanId, userId, BigDecimal.valueOf(100000), initialNextPaymentDate);
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(invocation -> {
            PaymentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        CreatePaymentRequest request = new CreatePaymentRequest(loanId, BigDecimal.valueOf(30000), PaymentType.PARTIAL);
        PaymentResponse response = paymentCreateUseCase.createPayment(user, request);

        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.valueOf(70000));
        assertThat(response.nextPaymentDate()).isEqualTo(initialNextPaymentDate.plusMonths(1));
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);

        ArgumentCaptor<PaymentEntity> captor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(70000));
    }

    @Test
    void createPayment_fullAmount_closesLoan() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = loan(loanId, userId, BigDecimal.valueOf(50000), LocalDate.now().plusMonths(1));
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanProvider.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreatePaymentRequest request = new CreatePaymentRequest(loanId, BigDecimal.valueOf(50000), PaymentType.FULL);
        PaymentResponse response = paymentCreateUseCase.createPayment(user, request);

        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
    }

    @Test
    void createPayment_amountExceedsRemaining_throwsAndDoesNotSave() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = loan(loanId, userId, BigDecimal.valueOf(50000), LocalDate.now().plusMonths(1));
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));

        CreatePaymentRequest request = new CreatePaymentRequest(loanId, BigDecimal.valueOf(60000), PaymentType.PARTIAL);

        assertThatThrownBy(() -> paymentCreateUseCase.createPayment(user, request))
                .isInstanceOf(PaymentExceedsBalanceException.class);
        verify(loanProvider, org.mockito.Mockito.never()).save(any());
        verify(paymentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void createPayment_loanNotOwnedByUser_throwsNotFound() {
        UUID loanId = UUID.randomUUID();
        when(loanProvider.findById(loanId)).thenReturn(Optional.empty());

        CreatePaymentRequest request = new CreatePaymentRequest(loanId, BigDecimal.valueOf(1000), PaymentType.PARTIAL);

        assertThatThrownBy(() -> paymentCreateUseCase.createPayment(user, request))
                .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void createPayment_closedLoan_throwsLoanNotActive() {
        UUID loanId = UUID.randomUUID();
        LoanEntity loan = loan(loanId, userId, BigDecimal.ZERO, null);
        loan.setStatus(LoanStatus.CLOSED);
        when(loanProvider.findById(loanId)).thenReturn(Optional.of(loan));

        CreatePaymentRequest request = new CreatePaymentRequest(loanId, BigDecimal.valueOf(1000), PaymentType.PARTIAL);

        assertThatThrownBy(() -> paymentCreateUseCase.createPayment(user, request))
                .isInstanceOf(LoanNotActiveException.class);
    }

    private LoanEntity loan(UUID id, UUID ownerId, BigDecimal remaining, LocalDate nextPaymentDate) {
        return LoanEntity.builder()
                .id(id)
                .userId(ownerId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(remaining)
                .remainingAmount(remaining)
                .termMonths(12)
                .nextPaymentDate(nextPaymentDate)
                .status(LoanStatus.ACTIVE)
                .build();
    }
}
