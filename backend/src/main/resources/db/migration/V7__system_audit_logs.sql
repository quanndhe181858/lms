CREATE TABLE system_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(120) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(60) NOT NULL,
    actor_user_id BIGINT,
    previous_state TEXT,
    new_state TEXT,
    details_json TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_entity ON system_audit_logs (entity_name, entity_id);
CREATE INDEX idx_audit_actor ON system_audit_logs (actor_user_id, created_at);
