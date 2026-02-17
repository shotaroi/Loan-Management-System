-- Customer table
CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_email ON customer(email);

-- Loan Application table
CREATE TABLE loan_application (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    principal DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    term_months INTEGER NOT NULL,
    annual_interest_rate DECIMAL(10, 8) NOT NULL,
    status VARCHAR(50) NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP,
    decision_reason VARCHAR(1000),
    CONSTRAINT chk_application_status CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_loan_application_customer ON loan_application(customer_id);
CREATE INDEX idx_loan_application_status ON loan_application(status);

-- Loan table
CREATE TABLE loan (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id) ON DELETE CASCADE,
    application_id BIGINT NOT NULL UNIQUE REFERENCES loan_application(id) ON DELETE RESTRICT,
    principal DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    term_months INTEGER NOT NULL,
    annual_interest_rate DECIMAL(10, 8) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    outstanding_principal DECIMAL(19, 2) NOT NULL,
    accrued_interest DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'CLOSED', 'DEFAULTED'))
);

CREATE INDEX idx_loan_customer ON loan(customer_id);
CREATE INDEX idx_loan_application ON loan(application_id);
CREATE INDEX idx_loan_status ON loan(status);

-- Repayment Schedule table
CREATE TABLE repayment_schedule (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loan(id) ON DELETE CASCADE,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    principal_due DECIMAL(19, 2) NOT NULL,
    interest_due DECIMAL(19, 2) NOT NULL,
    total_due DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    amount_paid DECIMAL(19, 2) DEFAULT 0,
    CONSTRAINT chk_schedule_status CHECK (status IN ('DUE', 'PAID', 'LATE')),
    CONSTRAINT uq_loan_installment UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_repayment_schedule_loan ON repayment_schedule(loan_id);
CREATE INDEX idx_repayment_schedule_due_date ON repayment_schedule(loan_id, due_date);

-- Payment table
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loan(id) ON DELETE CASCADE,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_date DATE NOT NULL,
    reference VARCHAR(255),
    allocated_to_interest DECIMAL(19, 2) NOT NULL DEFAULT 0,
    allocated_to_principal DECIMAL(19, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_loan ON payment(loan_id);

-- Audit Log table
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_customer_id BIGINT REFERENCES customer(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_actor ON audit_log(actor_customer_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
