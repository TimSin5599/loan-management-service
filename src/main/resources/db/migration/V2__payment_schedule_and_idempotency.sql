CREATE TABLE payment_schedule
(
    id                 UUID PRIMARY KEY,
    loan_id            UUID           NOT NULL REFERENCES user_loans (id),
    due_date           DATE           NOT NULL,
    amount             NUMERIC(19, 2) NOT NULL,
    installment_number INTEGER        NOT NULL,
    created_at         TIMESTAMP      NOT NULL,
    CONSTRAINT uq_payment_schedule_loan_installment UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_payment_schedule_loan_id ON payment_schedule (loan_id);

CREATE TABLE idempotency_record
(
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    operation       VARCHAR(100) NOT NULL,
    response_status INTEGER      NOT NULL,
    response_body   TEXT         NOT NULL,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency_record_key_operation UNIQUE (idempotency_key, operation)
);
