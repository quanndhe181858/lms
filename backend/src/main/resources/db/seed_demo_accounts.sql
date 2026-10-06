USE lms_db;

-- 1. Fix department of Tran Minh Quan (quanly@lms.local) to Business Operations (3)
UPDATE users SET department_id = 3 WHERE id = 5;

-- 2. Insert new demo accounts (Password: password -> $2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy)
INSERT INTO users (id, email, password_hash, full_name, department_id, manager_id, role, employment_status, hire_date, is_active, created_at, updated_at) VALUES
(7, 'nam.it@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Vũ Hoàng Nam', 1, 2, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-03-15', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'minh.tester@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Đặng Quang Minh', 1, 2, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-05-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'hoa.cskh@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Nguyễn Thị Mai Hoa', 3, 5, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-04-10', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'hai.sales@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Trần Đức Hải', 3, 5, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-06-20', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 'ha.hr@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Lê Thanh Hà', 2, 6, 'ROLE_EMPLOYEE', 'PERMANENT', '2024-02-01', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 'tuan.lead@lms.local', '$2a$12$2rgPF3EtsVfRrvYWvGDMDeSAVlfCEUiFdsC.N/jpZMisOy71UYrNy', 'Lê Anh Tuấn', 1, 2, 'ROLE_MANAGER', 'PERMANENT', '2024-01-15', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE 
    full_name = VALUES(full_name),
    department_id = VALUES(department_id),
    manager_id = VALUES(manager_id),
    role = VALUES(role);

-- 3. Seed Leave Balances for new users
INSERT INTO leave_balances (user_id, leave_type_id, accrued_days, carried_over_days, used_days, pending_days, version, created_at, updated_at) VALUES
-- User 7 (Vũ Hoàng Nam) - has 2 pending annual days
(7, 1, 12.00, 2.00, 0.00, 2.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User 8 (Đặng Quang Minh) - has 1 used sick day
(8, 1, 12.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 2, 10.00, 0.00, 1.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User 9 (Nguyễn Thị Mai Hoa) - has 1 pending sick day
(9, 1, 12.00, 1.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 2, 10.00, 0.00, 0.00, 1.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User 10 (Trần Đức Hải) - has 3 used annual days
(10, 1, 12.00, 2.00, 3.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User 11 (Lê Thanh Hà)
(11, 1, 12.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
-- User 12 (Lê Anh Tuấn) - has 2 pending annual days
(12, 1, 12.00, 3.00, 0.00, 2.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 2, 10.00, 0.00, 0.00, 0.00, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    accrued_days = VALUES(accrued_days),
    carried_over_days = VALUES(carried_over_days),
    used_days = VALUES(used_days),
    pending_days = VALUES(pending_days);

-- 4. Seed realistic Leave Requests for demo (current period Oct 2026)
INSERT INTO leave_requests (id, request_uuid, idempotency_key, user_id, leave_type_id, start_date, end_date, start_half, end_half, total_billable_days, reason, attachment_id, status, assigned_approver_id, rejection_reason, is_backdated, reminder_sent, submitted_at, escalated_at, resolved_at, created_at, updated_at) VALUES
(6, 'req-uuid-006', 'idem-key-006', 7, 1, '2026-10-08', '2026-10-09', 'MORNING', 'AFTERNOON', 2.00, 'Nghỉ giải quyết việc riêng gia đình', NULL, 'SUBMITTED', 2, NULL, FALSE, FALSE, '2026-10-06 08:30:00', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 'req-uuid-007', 'idem-key-007', 9, 2, '2026-10-09', '2026-10-09', 'MORNING', 'AFTERNOON', 1.00, 'Khám sức khỏe tổng quát theo lịch viện', NULL, 'SUBMITTED', 5, NULL, FALSE, FALSE, '2026-10-06 09:15:00', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 'req-uuid-008', 'idem-key-008', 10, 1, '2026-10-12', '2026-10-14', 'MORNING', 'AFTERNOON', 3.00, 'Nghỉ phép thường niên kết hợp du lịch gia đình', NULL, 'APPROVED', 5, NULL, FALSE, FALSE, '2026-10-01 10:00:00', NULL, '2026-10-02 14:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 'req-uuid-009', 'idem-key-009', 8, 2, '2026-10-07', '2026-10-07', 'MORNING', 'AFTERNOON', 1.00, 'Nghỉ ốm sốt vi-rút nhẹ', NULL, 'APPROVED', 2, NULL, FALSE, FALSE, '2026-10-06 20:00:00', NULL, '2026-10-07 07:30:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 'req-uuid-010', 'idem-key-010', 12, 1, '2026-10-15', '2026-10-16', 'MORNING', 'AFTERNOON', 2.00, 'Tham gia hội thảo công nghệ Tech Summit 2026', NULL, 'SUBMITTED', 2, NULL, FALSE, FALSE, '2026-10-06 14:00:00', NULL, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    reason = VALUES(reason),
    assigned_approver_id = VALUES(assigned_approver_id);
