CREATE TABLE foreign_key_constraint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_table_id BIGINT NOT NULL,
    from_column_name VARCHAR(255) NOT NULL,
    to_table_id BIGINT NOT NULL,
    to_column_name VARCHAR(255) NOT NULL
);
