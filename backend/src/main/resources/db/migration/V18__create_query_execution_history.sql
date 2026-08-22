CREATE TABLE query_execution_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    saved_query_id BIGINT,
    sql_text TEXT NOT NULL,
    connection_id BIGINT NOT NULL,
    schema_name VARCHAR(255) NOT NULL,
    params TEXT,
    result_row_count INT NOT NULL,
    execution_time_ms BIGINT NOT NULL,
    executed_by_user_id BIGINT NOT NULL,
    executed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_query_execution_history_executed_by ON query_execution_history (executed_by_user_id);
CREATE INDEX idx_query_execution_history_connection ON query_execution_history (connection_id);
CREATE INDEX idx_query_execution_history_executed_at ON query_execution_history (executed_at);
