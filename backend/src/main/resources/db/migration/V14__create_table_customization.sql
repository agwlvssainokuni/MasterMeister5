CREATE TABLE table_customization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    connection_id BIGINT NOT NULL,
    schema_name VARCHAR(255) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    default_sort_column VARCHAR(255),
    default_sort_direction VARCHAR(10)
);

CREATE INDEX idx_table_customization_lookup ON table_customization (connection_id, schema_name, table_name);
