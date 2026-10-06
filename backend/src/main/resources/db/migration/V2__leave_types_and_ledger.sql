CREATE TABLE leave_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_days_per_year DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    is_confidential_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    requires_attachment_days INT NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    accrued_days DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    carried_over_days DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    used_days DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    pending_days DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_balances_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_balances_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT uk_user_leave_type UNIQUE (user_id, leave_type_id)
);

CREATE TABLE leave_ledger_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_uuid VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    request_id BIGINT NULL,
    amount DECIMAL(5, 2) NOT NULL,
    entry_type VARCHAR(30) NOT NULL,
    balance_after DECIMAL(5, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ledger_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_ledger_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX idx_ledger_user_type ON leave_ledger_entries(user_id, leave_type_id);
