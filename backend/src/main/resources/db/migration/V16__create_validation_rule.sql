CREATE TABLE validation_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    column_customization_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL,
    pattern VARCHAR(500),
    min_value VARCHAR(255),
    max_value VARCHAR(255)
);

CREATE INDEX idx_validation_rule_column ON validation_rule (column_customization_id);
