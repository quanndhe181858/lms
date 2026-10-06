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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_leave_attachment_owner ON leave_attachments (owner_user_id);
CREATE INDEX idx_leave_attachment_uuid ON leave_attachments (file_uuid);
