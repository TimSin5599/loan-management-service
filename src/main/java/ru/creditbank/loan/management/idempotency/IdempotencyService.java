package ru.creditbank.loan.management.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class IdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository, ObjectMapper objectMapper) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> findCached(String idempotencyKey, String operation, Class<T> responseType) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return Optional.empty();
        }
        return idempotencyRecordRepository.findByIdempotencyKeyAndOperation(idempotencyKey, operation)
                .map(record -> deserialize(record.getResponseBody(), responseType));
    }

    @SuppressWarnings("unchecked")
    public <T> T remember(String idempotencyKey, String operation, T response) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return response;
        }
        IdempotencyRecordEntity record = IdempotencyRecordEntity.builder()
                .idempotencyKey(idempotencyKey)
                .operation(operation)
                .responseStatus(200)
                .responseBody(serialize(response))
                .build();
        try {
            idempotencyRecordRepository.save(record);
            return response;
        } catch (DataIntegrityViolationException ex) {
            log.info("Гонка при сохранении идемпотентного ответа, возвращаем ответ конкурирующего запроса: idempotencyKey={}, operation={}",
                    idempotencyKey, operation);
            return idempotencyRecordRepository.findByIdempotencyKeyAndOperation(idempotencyKey, operation)
                    .map(existing -> deserialize(existing.getResponseBody(), (Class<T>) response.getClass()))
                    .orElseThrow(() -> ex);
        }
    }

    private <T> T deserialize(String json, Class<T> responseType) {
        try {
            return objectMapper.readValue(json, responseType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось десериализовать закэшированный идемпотентный ответ", e);
        }
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать идемпотентный ответ", e);
        }
    }
}
