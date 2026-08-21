CREATE TABLE audit_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_user_id BIGINT,
    target_user_id BIGINT,
    details TEXT,
    correlation_id VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL
);
