CREATE TABLE db_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schema_id BIGINT NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    table_type VARCHAR(20) NOT NULL,
    comment VARCHAR(1000)
);
