CREATE TABLE user_loans
(
    id                    UUID PRIMARY KEY,
    user_id               UUID           NOT NULL,
    credit_application_id UUID           NOT NULL UNIQUE,
    total_amount          NUMERIC(19, 2) NOT NULL,
    remaining_amount      NUMERIC(19, 2) NOT NULL,
    interest_rate         NUMERIC(5, 2),
    term_months           INTEGER        NOT NULL,
    next_payment_date     DATE,
    status                VARCHAR(20)    NOT NULL,
    created_at            TIMESTAMP      NOT NULL,
    updated_at            TIMESTAMP      NOT NULL
);

CREATE INDEX idx_user_loans_user_id ON user_loans (user_id);

CREATE TABLE payments
(
    id            UUID PRIMARY KEY,
    loan_id       UUID           NOT NULL REFERENCES user_loans (id),
    amount        NUMERIC(19, 2) NOT NULL,
    payment_type  VARCHAR(20)    NOT NULL,
    payment_date  TIMESTAMPTZ    NOT NULL,
    balance_after NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_payments_loan_id ON payments (loan_id);
