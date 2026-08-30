package ru.creditbank.loan.management.loan.cancel.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelLoanControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Value("${internal-api.key}")
    private String internalApiKey;

    @Test
    void cancelLoan_activeLoan_setsStatusToCancelledInDb() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(LoanStatus.ACTIVE));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/cancel";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(loan.getId().toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        LoanEntity updated = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_calledTwice_isIdempotentNoOp() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(LoanStatus.ACTIVE));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/cancel";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk());
        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        LoanEntity updated = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_withIdempotencyKey_returnsSameResponseOnRetry() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(LoanStatus.ACTIVE));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/cancel";
        String idempotencyKey = UUID.randomUUID().toString();

        String firstResponse = mockMvc.perform(post(endpoint)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post(endpoint)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .header("Idempotency-Key", idempotencyKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(secondResponse).isEqualTo(firstResponse);
    }

    @Test
    void cancelLoan_closedLoan_returnsConflict() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(LoanStatus.CLOSED));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/cancel";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isConflict());

        LoanEntity unchanged = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(LoanStatus.CLOSED);
    }

    @Test
    void cancelLoan_loanMissing_returnsNotFound() throws Exception {
        String endpoint = "/loan-management-service/internal/loans/" + UUID.randomUUID() + "/cancel";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound());
    }

    private LoanEntity loanFor(LoanStatus status) {
        BigDecimal amount = BigDecimal.valueOf(50000);
        return LoanEntity.builder()
                .userId(UUID.randomUUID())
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(amount)
                .remainingAmount(amount)
                .termMonths(12)
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .status(status)
                .build();
    }
}
