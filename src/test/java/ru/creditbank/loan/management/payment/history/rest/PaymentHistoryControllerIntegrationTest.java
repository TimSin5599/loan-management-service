package ru.creditbank.loan.management.payment.history.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.payment.dao.entity.PaymentEntity;
import ru.creditbank.loan.management.payment.dao.entity.PaymentType;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;
import ru.creditbank.loan.management.support.JwtTestTokenFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentHistoryControllerIntegrationTest extends IntegrationTestBase {
    private static final String ENDPOINT = "/loan-management-service/api/payment/history";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void getHistory_withExistingPayments_returnsThemForOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(userId));
        paymentRepository.save(paymentFor(loan.getId(), BigDecimal.valueOf(20000)));
        paymentRepository.save(paymentFor(loan.getId(), BigDecimal.valueOf(15000)));
        String token = JwtTestTokenFactory.generateToken(jwtSecret, userId, "user@example.com", null);

        mockMvc.perform(get(ENDPOINT)
                        .param("loanId", loan.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments.length()").value(2));
    }

    @Test
    void getHistory_forSomeoneElsesLoan_returnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(ownerId));
        String token = JwtTestTokenFactory.generateToken(jwtSecret, UUID.randomUUID(), "other@example.com", null);

        mockMvc.perform(get(ENDPOINT)
                        .param("loanId", loan.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHistory_missingLoanIdParam_returnsBadRequest() throws Exception {
        String token = JwtTestTokenFactory.generateToken(jwtSecret, UUID.randomUUID(), "user@example.com", null);

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHistory_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("loanId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    private LoanEntity loanFor(UUID userId) {
        return LoanEntity.builder()
                .userId(userId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(150000))
                .remainingAmount(BigDecimal.valueOf(115000))
                .termMonths(12)
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .build();
    }

    private PaymentEntity paymentFor(UUID loanId, BigDecimal amount) {
        return PaymentEntity.builder()
                .loanId(loanId)
                .amount(amount)
                .paymentType(PaymentType.PARTIAL)
                .balanceAfter(BigDecimal.valueOf(100000))
                .build();
    }
}
