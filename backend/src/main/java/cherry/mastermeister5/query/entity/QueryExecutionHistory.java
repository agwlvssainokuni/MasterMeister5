/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.query.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;

/**
 * domain-entities.md (Unit 6) QueryExecutionHistory. BR-11: one row per
 * execution; {@code sqlText} is a snapshot at execution time, unaffected by
 * later edits to the originating {@link SavedQuery}.
 */
@Entity
@Table(name = "query_execution_history")
public class QueryExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long savedQueryId;

    @Column(nullable = false, columnDefinition = "text")
    private String sqlText;

    @Column(nullable = false)
    private Long connectionId;

    @Column(nullable = false)
    private String schemaName;

    @Convert(converter = QueryParamsJsonConverter.class)
    @Column(columnDefinition = "text")
    private Map<String, Object> params;

    @Column(nullable = false)
    private int resultRowCount;

    @Column(nullable = false)
    private long executionTimeMs;

    @Column(nullable = false)
    private Long executedByUserId;

    @Column(nullable = false)
    private Instant executedAt;

    protected QueryExecutionHistory() {
    }

    public QueryExecutionHistory(
            Long savedQueryId,
            String sqlText,
            Long connectionId,
            String schemaName,
            Map<String, Object> params,
            int resultRowCount,
            long executionTimeMs,
            Long executedByUserId) {
        this.savedQueryId = savedQueryId;
        this.sqlText = sqlText;
        this.connectionId = connectionId;
        this.schemaName = schemaName;
        this.params = params;
        this.resultRowCount = resultRowCount;
        this.executionTimeMs = executionTimeMs;
        this.executedByUserId = executedByUserId;
        this.executedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getSavedQueryId() {
        return savedQueryId;
    }

    public String getSqlText() {
        return sqlText;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public int getResultRowCount() {
        return resultRowCount;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public Long getExecutedByUserId() {
        return executedByUserId;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }
}
