-- init_mysql.sql: Full MySQL Schema & Seed Data for LMS (Leave Management System)

CREATE DATABASE IF NOT EXISTS lms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lms_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS system_audit_logs;
DROP TABLE IF EXISTS leave_attachments;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS leave_ledger_entries;
DROP TABLE IF EXISTS leave_balances;
DROP TABLE IF EXISTS leave_types;
DROP TABLE IF EXISTS public_holidays;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. Departments Table
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    head_user_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Users Table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    department_id BIGINT NOT NULL,
    manager_id BIGINT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_EMPLOYEE',
    employment_status VARCHAR(30) NOT NULL DEFAULT 'PROBATION',
    hire_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_manager ON users(manager_id);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_role ON users(role);

-- Add circular head_user_id FK reference after users table creation
ALTER TABLE departments ADD CONSTRAINT fk_departments_head_user FOREIGN KEY (head_user_id) REFERENCES users(id);

-- 3. Leave Types Table
CREATE TABLE leave_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    default_days_per_year DECIMAL(5, 2) NOT NULL DEFAULT 12.00,
    is_confidential_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    requires_attachment_days INT NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Leave Balances Table
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_balances_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_balances_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT uk_user_leave_type UNIQUE (user_id, leave_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Leave Ledger Entries Table
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ledger_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_ledger_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ledger_user_type ON leave_ledger_entries(user_id, leave_type_id);

-- 6. Leave Requests Table
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_requests_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_requests_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT fk_requests_approver FOREIGN KEY (assigned_approver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_requests_user_status ON leave_requests(user_id, status);
CREATE INDEX idx_requests_approver_status ON leave_requests(assigned_approver_id, status);
CREATE INDEX idx_requests_sla_lookup ON leave_requests(status, submitted_at, reminder_sent);

-- 7. Leave Attachments Table
CREATE TABLE leave_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_uuid VARCHAR(64) NOT NULL UNIQUE,
    owner_user_id BIGINT NOT NULL,
    request_id BIGINT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    is_confidential BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_leave_attachment_owner ON leave_attachments (owner_user_id);
CREATE INDEX idx_leave_attachment_uuid ON leave_attachments (file_uuid);

-- 8. Public Holidays Table
CREATE TABLE public_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    calendar_year INT NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. System Audit Logs Table
CREATE TABLE system_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(120) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(60) NOT NULL,
    actor_user_id BIGINT NULL,
    previous_state TEXT NULL,
    new_state TEXT NULL,
    details_json TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_entity ON system_audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_actor ON system_audit_logs (actor_user_id, created_at);

-- ── SEED DATA ─────────────────────────────────────────────────────────────────

-- Seed Departments
INSERT INTO departments (id, name, department_code, created_at, updated_at) VALUES 
(1, 'Engineering', 'ENG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'People Operations', 'HR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Business Operations', 'OPS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Password for all accounts: password
-- BCrypt Work Factor 12 Hash: $2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy
INSERT INTO users (id, email, password_hash, full_name, department_id, manager_id, role, employment_status, hire_date, is_active, created_at, updated_at) VALUES 
(1, 'admin@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'System Admin', 2, NULL, 'ROLE_HR_ADMIN', 'PERMANENT', '2024-01-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'david.manager@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'David Manager', 1, 1, 'ROLE_MANAGER', 'PERMANENT', '2024-03-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'sarah.engineer@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Sarah Engineer', 1, 2, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-06-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'nhanvien@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Nguyễn Thảo My', 3, 5, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-02-15', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'quanly@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Trần Minh Quân', 2, 1, 'ROLE_MANAGER', 'PERMANENT', '2024-01-10', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'hr@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Phạm Hoài Nhi', 2, 1, 'ROLE_HR_ADMIN', 'PERMANENT', '2024-01-05', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE departments SET head_user_id = 2 WHERE id = 1;
UPDATE departments SET head_user_id = 1 WHERE id = 2;
UPDATE departments SET head_user_id = 5 WHERE id = 3;

-- Seed Leave Types
INSERT INTO leave_types (id, code, name, default_days_per_year, is_confidential_attachment, requires_attachment_days, is_active, created_at, updated_at) VALUES 
(1, 'ANNUAL', 'Annual Leave', 12.00, FALSE, 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'SICK', 'Sick Leave', 10.00, TRUE, 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'UNPAID', 'Unpaid Leave', 0.00, FALSE, 0, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'MATERNITY', 'Maternity Leave', 180.00, TRUE, 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Leave Balances for all users
INSERT INTO leave_balances (user_id, leave_type_id, accrued_days, carried_over_days, used_days, pending_days, version, created_at, updated_at) VALUES 
(1, 1, 12.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 12.00, 2.00, 1.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 10.00, 0.00, 3.00, 1.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 12.00, 1.00, 2.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 10.00, 0.00, 1.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, 12.00, 3.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 1, 12.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Public Holidays
INSERT INTO public_holidays (holiday_date, name, calendar_year, description) VALUES
('2026-01-01', 'New Year\'s Day', 2026, 'New Year\'s Day'),
('2026-02-08', 'Tet Holiday', 2026, 'Lunar New Year holiday'),
('2026-09-02', 'National Day', 2026, 'National Day holiday');

-- Seed Sample Leave Requests
INSERT INTO leave_requests (request_uuid, idempotency_key, user_id, leave_type_id, start_date, end_date, start_half, end_half, total_billable_days, reason, attachment_id, status, assigned_approver_id, rejection_reason, is_backdated, reminder_sent, submitted_at, escalated_at, resolved_at, created_at, updated_at) VALUES
('req-uuid-001', 'idem-key-001', 3, 1, '2026-10-10', '2026-10-12', 'MORNING', 'AFTERNOON', 3.00, 'Nghỉ phép gia đình', NULL, 'SUBMITTED', 2, NULL, FALSE, FALSE, CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('req-uuid-002', 'idem-key-002', 4, 2, '2026-10-01', '2026-10-02', 'MORNING', 'AFTERNOON', 2.00, 'Nghỉ ốm có xác nhận y tế', NULL, 'APPROVED', 5, NULL, FALSE, FALSE, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
