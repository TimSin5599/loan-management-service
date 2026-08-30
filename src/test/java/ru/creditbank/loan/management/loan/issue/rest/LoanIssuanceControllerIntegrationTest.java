package ru.creditbank.loan.management.loan.issue.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.creditbank.loan.management.loan.dao.repository.LoanRepository;
import ru.creditbank.loan.management.support.IntegrationTestBase;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanIssuanceControllerIntegrationTest extends IntegrationTestBase {
    private static final String ENDPOINT = "/loan-management-service/internal/loans";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRepository loanRepository;

    @Value("${internal-api.key}")
    private String internalApiKey;

    @Test
    void issueLoan_withValidKey_createsLoanInDb() throws Exception {
        UUID creditApplicationId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "creditApplicationId", creditApplicationId.toString(),
                "userId", UUID.randomUUID().toString(),
                "totalAmount", 100000.00,
                "termMonths", 24,
                "interestRate", 12.5
        );

        String responseJson = mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loanId").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        UUID loanId = UUID.fromString(objectMapper.readTree(responseJson).get("loanId").asText());
        assertThat(loanRepository.findById(loanId)).isPresent();
        assertThat(loanRepository.findByCreditApplicationId(creditApplicationId)).isPresent();
    }

    @Test
    void issueLoan_calledTwiceForSameApplication_isIdempotent() throws Exception {
        UUID creditApplicationId = UUID.randomUUID();
        Map<String, Object> requestBody = Map.of(
                "creditApplicationId", creditApplicationId.toString(),
                "userId", UUID.randomUUID().toString(),
                "totalAmount", 50000.00,
                "termMonths", 6
        );

        String firstResponse = mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String secondResponse = mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", internalApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String firstLoanId = objectMapper.readTree(firstResponse).get("loanId").asText();
        String secondLoanId = objectMapper.readTree(secondResponse).get("loanId").asText();
        assertThat(secondLoanId).isEqualTo(firstLoanId);
        assertThat(loanRepository.findByCreditApplicationId(creditApplicationId)).isPresent();
    }

    @Test
    void issueLoan_withInvalidKey_returnsUnauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "creditApplicationId", UUID.randomUUID().toString(),
                "userId", UUID.randomUUID().toString(),
                "totalAmount", 50000.00,
                "termMonths", 6
        );

        mockMvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void issueLoan_withoutKey_returnsUnauthorized() throws Exception {
        Map<String, Object> requestBody = Map.of(
                "creditApplicationId", UUID.randomUUID().toString(),
                "userId", UUID.randomUUID().toString(),
                "totalAmount", 50000.00,
                "termMonths", 6
        );

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }
}
