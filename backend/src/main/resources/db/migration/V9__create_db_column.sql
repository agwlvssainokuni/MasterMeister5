CREATE TABLE db_column (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_id BIGINT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    ordinal_position INT NOT NULL,
    data_type VARCHAR(255) NOT NULL,
    nullable BOOLEAN NOT NULL,
    primary_key BOOLEAN NOT NULL,
    comment VARCHAR(1000)
);
