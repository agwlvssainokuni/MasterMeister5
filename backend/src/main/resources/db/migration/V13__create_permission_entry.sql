CREATE TABLE permission_entry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_type VARCHAR(20) NOT NULL,
    subject_id BIGINT NOT NULL,
    connection_id BIGINT NOT NULL,
    resource_level VARCHAR(20) NOT NULL,
    schema_name VARCHAR(255) NOT NULL,
    table_name VARCHAR(255),
    column_name VARCHAR(255),
    primary_level VARCHAR(20),
    aux_create BOOLEAN,
    aux_delete BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_permission_entry_lookup ON permission_entry (connection_id, schema_name, subject_type, subject_id);
