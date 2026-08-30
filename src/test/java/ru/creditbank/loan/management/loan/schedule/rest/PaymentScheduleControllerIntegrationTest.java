package ru.creditbank.loan.management.loan.schedule.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.entity.LoanEntity;
import ru.creditbank.loan.management.loan.dao.entity.LoanStatus;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.loan.schedule.dao.entity.PaymentScheduleEntity;
import ru.creditbank.loan.management.loan.schedule.dao.repository.PaymentScheduleRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentScheduleControllerIntegrationTest extends IntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;

    @Value("${internal-api.key}")
    private String internalApiKey;

    @Test
    void createSchedule_freshLoan_createsInstallmentsInDb() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(BigDecimal.valueOf(100000), 3));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/schedule";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").value(loan.getId().toString()))
                .andExpect(jsonPath("$.installmentsCreated").value(3));

        List<PaymentScheduleEntity> installments = paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        assertThat(installments).hasSize(3);
        BigDecimal sum = installments.stream().map(PaymentScheduleEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(BigDecimal.valueOf(100000));
    }

    @Test
    void createSchedule_calledTwice_doesNotDuplicateRows() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(BigDecimal.valueOf(60000), 6));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/schedule";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk());
        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentsCreated").value(6));

        List<PaymentScheduleEntity> installments = paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        assertThat(installments).hasSize(6);
    }

    @Test
    void createSchedule_withIdempotencyKey_returnsSameResponseOnRetry() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(BigDecimal.valueOf(90000), 4));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/schedule";
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
        List<PaymentScheduleEntity> installments = paymentScheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loan.getId());
        assertThat(installments).hasSize(4);
    }

    @Test
    void createSchedule_loanMissing_returnsNotFound() throws Exception {
        String endpoint = "/loan-management-service/internal/loans/" + UUID.randomUUID() + "/schedule";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSchedule_loanCancelled_returnsConflict() throws Exception {
        LoanEntity loan = loanRepository.save(loanFor(BigDecimal.valueOf(50000), 5, LoanStatus.CANCELLED));
        String endpoint = "/loan-management-service/internal/loans/" + loan.getId() + "/schedule";

        mockMvc.perform(post(endpoint).header("X-Internal-Api-Key", internalApiKey))
                .andExpect(status().isConflict());
    }

    private LoanEntity loanFor(BigDecimal totalAmount, int termMonths) {
        return loanFor(totalAmount, termMonths, LoanStatus.ACTIVE);
    }

    private LoanEntity loanFor(BigDecimal totalAmount, int termMonths, LoanStatus status) {
        return LoanEntity.builder()
                .userId(UUID.randomUUID())
                .creditApplicationId(UUID.randomUUID())
                .totalAmount(totalAmount)
                .remainingAmount(totalAmount)
                .termMonths(termMonths)
                .nextPaymentDate(LocalDate.now().plusMonths(1))
                .status(status)
                .build();
    }
}
