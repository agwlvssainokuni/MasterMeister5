CREATE TABLE db_schema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    schema_name VARCHAR(255) NOT NULL,
    imported_at TIMESTAMP NOT NULL
);
