package ru.creditbank.loan.management.payment.create.rest.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.creditbank.loan.management.payment.dao.entity.PaymentType;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePaymentRequestValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), BigDecimal.valueOf(50000), PaymentType.PARTIAL);

        Set<ConstraintViolation<CreatePaymentRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullLoanId_isRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(null, BigDecimal.valueOf(50000), PaymentType.PARTIAL);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void nullAmount_isRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), null, PaymentType.PARTIAL);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void nonPositiveAmount_isRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), BigDecimal.ZERO, PaymentType.PARTIAL);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void nullPaymentType_isRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), BigDecimal.valueOf(50000), null);

        assertThat(validator.validate(request)).isNotEmpty();
    }
}
