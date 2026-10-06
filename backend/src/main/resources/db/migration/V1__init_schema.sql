-- V1__init_schema.sql: Core Departments, Users and Seed Data

CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_code VARCHAR(20) NOT NULL UNIQUE,
    head_user_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES users(id)
);

CREATE INDEX idx_users_manager ON users(manager_id);
CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_role ON users(role);

-- Seed Baseline Departments
INSERT INTO departments (id, name, department_code, created_at, updated_at) VALUES 
(1, 'Engineering', 'ENG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'People Operations', 'HR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Business Operations', 'OPS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Seed Baseline Users with BCrypt hashes (Default password: password)
-- BCrypt Work Factor 12 Hash: $2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy
INSERT INTO users (id, email, password_hash, full_name, department_id, manager_id, role, employment_status, hire_date, is_active, created_at, updated_at) VALUES 
(1, 'admin@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'System Admin', 2, NULL, 'ROLE_HR_ADMIN', 'PERMANENT', '2024-01-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'david.manager@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'David Manager', 1, 1, 'ROLE_MANAGER', 'PERMANENT', '2024-03-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'sarah.engineer@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Sarah Engineer', 1, 2, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-06-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 'nhanvien@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Nguyễn Thảo My', 3, 2, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-02-15', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 'quanly@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Trần Minh Quân', 2, 1, 'ROLE_MANAGER', 'PERMANENT', '2024-01-10', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 'hr@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Phạm Hoài Nhi', 2, 1, 'ROLE_HR_ADMIN', 'PERMANENT', '2024-01-05', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

UPDATE departments SET head_user_id = 2 WHERE id = 1;
UPDATE departments SET head_user_id = 1 WHERE id = 2;
UPDATE departments SET head_user_id = 5 WHERE id = 3;
