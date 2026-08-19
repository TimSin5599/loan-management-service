package ru.creditbank.loan.management.loan.list.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;
import ru.creditbank.loan.management.support.JwtTestTokenFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanListControllerIntegrationTest extends IntegrationTestBase {

    private static final String ENDPOINT = "/loan-management-service/api/v1/payment/loans";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void getUserLoans_withExistingLoans_returnsOnlyLoansOfCurrentUser() throws Exception {
        UUID userId = UUID.randomUUID();
        LoanEntity ownLoan = loanRepository.save(loanFor(userId, BigDecimal.valueOf(150000)));
        loanRepository.save(loanFor(UUID.randomUUID(), BigDecimal.valueOf(90000)));

        String token = JwtTestTokenFactory.generateToken(jwtSecret, userId, "user@example.com", null);

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans.length()").value(1))
                .andExpect(jsonPath("$.loans[0].loanId").value(ownLoan.getId().toString()))
                .andExpect(jsonPath("$.loans[0].totalAmount").value(150000.00))
                .andExpect(jsonPath("$.loans[0].status").value("ACTIVE"));
    }

    @Test
    void getUserLoans_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getUserLoans_withNoLoans_returnsEmptyList() throws Exception {
        String token = JwtTestTokenFactory.generateToken(jwtSecret, UUID.randomUUID(), "empty@example.com", null);

        mockMvc.perform(get(ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loans.length()").value(0));
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
