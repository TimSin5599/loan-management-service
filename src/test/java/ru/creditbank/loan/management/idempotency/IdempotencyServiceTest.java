package ru.creditbank.loan.management.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {
    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private IdempotencyService idempotencyService;

    record SampleResponse(UUID id, String status) {
    }

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(idempotencyRecordRepository, objectMapper);
    }

    @Test
    void findCached_cacheMiss_returnsEmpty() {
        when(idempotencyRecordRepository.findByIdempotencyKeyAndOperation("key-1", "op-1"))
                .thenReturn(Optional.empty());

        Optional<SampleResponse> result = idempotencyService.findCached("key-1", "op-1", SampleResponse.class);

        assertThat(result).isEmpty();
    }

    @Test
    void remember_thenFindCached_returnsDeserializedMatchingType() throws Exception {
        SampleResponse response = new SampleResponse(UUID.randomUUID(), "ACTIVE");
        String json = objectMapper.writeValueAsString(response);

        when(idempotencyRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SampleResponse remembered = idempotencyService.remember("key-1", "op-1", response);
        assertThat(remembered).isEqualTo(response);

        IdempotencyRecordEntity stored = IdempotencyRecordEntity.builder()
                .idempotencyKey("key-1")
                .operation("op-1")
                .responseStatus(200)
                .responseBody(json)
                .build();
        when(idempotencyRecordRepository.findByIdempotencyKeyAndOperation("key-1", "op-1"))
                .thenReturn(Optional.of(stored));

        Optional<SampleResponse> cached = idempotencyService.findCached("key-1", "op-1", SampleResponse.class);

        assertThat(cached).contains(response);
    }

    @Test
    void findCached_sameKeyDifferentOperation_isCacheMiss() {
        when(idempotencyRecordRepository.findByIdempotencyKeyAndOperation("key-1", "other-op"))
                .thenReturn(Optional.empty());

        Optional<SampleResponse> result = idempotencyService.findCached("key-1", "other-op", SampleResponse.class);

        assertThat(result).isEmpty();
    }

    @Test
    void remember_concurrentInsertRace_returnsWinnersCachedResponseInsteadOfThrowing() throws Exception {
        SampleResponse ourResponse = new SampleResponse(UUID.randomUUID(), "ACTIVE");
        SampleResponse winnerResponse = new SampleResponse(UUID.randomUUID(), "ACTIVE");
        String winnerJson = objectMapper.writeValueAsString(winnerResponse);

        when(idempotencyRecordRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        IdempotencyRecordEntity winnerRecord = IdempotencyRecordEntity.builder()
                .idempotencyKey("key-1")
                .operation("op-1")
                .responseStatus(200)
                .responseBody(winnerJson)
                .build();
        when(idempotencyRecordRepository.findByIdempotencyKeyAndOperation("key-1", "op-1"))
                .thenReturn(Optional.of(winnerRecord));

        SampleResponse result = idempotencyService.remember("key-1", "op-1", ourResponse);

        assertThat(result).isEqualTo(winnerResponse);
    }
}
