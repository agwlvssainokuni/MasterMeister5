CREATE TABLE column_customization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_customization_id BIGINT NOT NULL,
    column_name VARCHAR(255) NOT NULL,
    display_label VARCHAR(255),
    display_order INT,
    hidden BOOLEAN NOT NULL,
    read_only BOOLEAN NOT NULL,
    input_widget VARCHAR(20),
    select_options_json VARCHAR(2000)
);

CREATE INDEX idx_column_customization_table ON column_customization (table_customization_id);
