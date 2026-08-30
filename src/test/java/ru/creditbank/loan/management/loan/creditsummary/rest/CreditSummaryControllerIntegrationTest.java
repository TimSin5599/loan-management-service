package ru.creditbank.loan.management.loan.creditsummary.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreditSummaryControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Value("${internal-api.key}")
    private String internalApiKey;

    @Test
    void getCreditSummary_withValidKey_returnsAggregatedSummary() throws Exception {
        UUID userId = UUID.randomUUID();
        loanRepository.save(loan(userId, LoanStatus.ACTIVE, BigDecimal.valueOf(50_000)));
        loanRepository.save(loan(userId, LoanStatus.OVERDUE, BigDecimal.valueOf(30_000)));
        loanRepository.save(loan(userId, LoanStatus.CLOSED, BigDecimal.ZERO));

        mockMvc.perform(get("/loan-management-service/internal/users/{userId}/payment-history", userId)
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(3))
                .andExpect(jsonPath("$.activeLoans").value(2))
                .andExpect(jsonPath("$.hasActiveOverdue").value(true))
                .andExpect(jsonPath("$.totalOutstandingDebt").value(80_000));
    }

    @Test
    void getCreditSummary_noLoansForUser_returnsZeroedSummary() throws Exception {
        mockMvc.perform(get("/loan-management-service/internal/users/{userId}/payment-history", UUID.randomUUID())
                        .header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoans").value(0))
                .andExpect(jsonPath("$.hasActiveOverdue").value(false));
    }

    @Test
    void getCreditSummary_withInvalidKey_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loan-management-service/internal/users/{userId}/payment-history", UUID.randomUUID())
                        .header("X-Internal-Api-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCreditSummary_withoutKey_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/loan-management-service/internal/users/{userId}/payment-history", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private LoanEntity loan(UUID userId, LoanStatus status, BigDecimal remainingAmount) {
        return LoanEntity.builder()
                .userId(userId)
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(BigDecimal.valueOf(100_000))
                .remainingAmount(remainingAmount)
                .termMonths(12)
                .status(status)
                .build();
    }
}
