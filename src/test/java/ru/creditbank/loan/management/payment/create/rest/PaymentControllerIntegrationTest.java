package ru.creditbank.loan.management.payment.create.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.payment.dao.repository.PaymentRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;
import ru.creditbank.loan.management.support.JwtTestTokenFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerIntegrationTest extends IntegrationTestBase {

    private static final String ENDPOINT = "/loan-management-service/api/v1/payment";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void createPayment_partialPayment_savesPaymentAndUpdatesLoanBalance() throws Exception {
        UUID userId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(userId, BigDecimal.valueOf(150000)));
        String token = generateToken(userId);

        Map<String, Object> requestBody = Map.of(
                "loanId", loan.getId().toString(),
                "amount", 50000.00,
                "paymentType", "PARTIAL"
        );

        String responseJson = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").exists())
                .andExpect(jsonPath("$.newBalance").value(100000.00))
                .andExpect(jsonPath("$.nextPaymentDate").exists())
                .andReturn().getResponse().getContentAsString();

        UUID paymentId = UUID.fromString(objectMapper.readTree(responseJson).get("paymentId").asText());
        assertThat(paymentRepository.findById(paymentId)).isPresent();

        LoanEntity updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updatedLoan.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(updatedLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void createPayment_fullRepayment_closesLoan() throws Exception {
        UUID userId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(userId, BigDecimal.valueOf(50000)));
        String token = generateToken(userId);

        Map<String, Object> requestBody = Map.of(
                "loanId", loan.getId().toString(),
                "amount", 50000.00,
                "paymentType", "FULL"
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance").value(0.00));

        LoanEntity updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updatedLoan.getStatus()).isEqualTo(LoanStatus.CLOSED);
    }

    @Test
    void createPayment_amountGreaterThanRemaining_returnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(userId, BigDecimal.valueOf(50000)));
        String token = generateToken(userId);

        Map<String, Object> requestBody = Map.of(
                "loanId", loan.getId().toString(),
                "amount", 60000.00,
                "paymentType", "PARTIAL"
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        LoanEntity unchangedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(unchangedLoan.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void createPayment_forSomeoneElsesLoan_returnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        LoanEntity loan = loanRepository.save(loanFor(ownerId, BigDecimal.valueOf(50000)));
        String token = generateToken(UUID.randomUUID());

        Map<String, Object> requestBody = Map.of(
                "loanId", loan.getId().toString(),
                "amount", 1000.00,
                "paymentType", "PARTIAL"
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPayment_withoutJwt_returnsUnauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "loanId", UUID.randomUUID().toString(),
                "amount", 1000.00,
                "paymentType", "PARTIAL"
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPayment_withInvalidData_returnsBadRequest() throws Exception {
        String token = generateToken(UUID.randomUUID());

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private String generateToken(UUID userId) {
        return JwtTestTokenFactory.generateToken(jwtSecret, userId, "user@example.com", null);
    }

    private LoanEntity loanFor(UUID userId, BigDecimal amount) {
        return LoanEntity.builder()
                .userId(userId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(amount)
                .remainingAmount(amount)
                .termMonths(12)
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .status(LoanStatus.ACTIVE)
                .build();
    }
}
