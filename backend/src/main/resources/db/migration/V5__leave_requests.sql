CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_uuid VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(64) NULL UNIQUE,
    user_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_half VARCHAR(20) NOT NULL DEFAULT 'MORNING',
    end_half VARCHAR(20) NOT NULL DEFAULT 'AFTERNOON',
    total_billable_days DECIMAL(4, 2) NOT NULL,
    reason TEXT NOT NULL,
    attachment_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    assigned_approver_id BIGINT NULL,
    rejection_reason TEXT NULL,
    is_backdated BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    escalated_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_requests_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_requests_approver FOREIGN KEY (assigned_approver_id) REFERENCES users(id)
);

CREATE INDEX idx_requests_user_status ON leave_requests(user_id, status);
CREATE INDEX idx_requests_approver_status ON leave_requests(assigned_approver_id, status);
CREATE INDEX idx_requests_sla_lookup ON leave_requests(status, submitted_at, reminder_sent);
